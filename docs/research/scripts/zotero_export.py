#!/usr/bin/env python3
"""
zotero_export.py

Copies (never moves) PDFs into a flat, browsable research folder with
human-readable filenames, plus a CSV index. Two sources, both copy-only:

1. Zotero library (--zotero-dir, on by default): reads a temp copy of
   zotero.sqlite for real citation metadata (title/author/year/tags),
   never touches your live Zotero storage or database.
2. Plain folders of PDFs (--extra-dir, repeatable): recursively finds
   *.pdf under each path and copies them in too, for research material
   that never went through Zotero. Filenames are lightly cleaned up
   (invalid-on-Windows characters stripped) but otherwise left as-is,
   since these are usually already reasonably APA-ish.

Re-running is safe: it loads the existing index.csv (if present) and
skips any file whose SHA-1 content hash already exists in --out-dir, so
you can run the Zotero pass and an --extra-dir pass separately, or run
either again later, without duplicating papers that show up in both.

CLOSE ZOTERO BEFORE RUNNING THIS if using the Zotero source. zotero.sqlite
is locked while Zotero is running; this script copies the DB file first,
but a live Zotero can still hold a lock that causes the copy step to fail
or grab a half-written snapshot mid-transaction.

Usage:
    # Zotero only (as before)
    python3 zotero_export.py --dry-run
    python3 zotero_export.py

    # Add a plain folder of PDFs to the same index
    python3 zotero_export.py --skip-zotero --extra-dir "/mnt/c/Users/nils/Documents/academic/courses/lu/25VT/SPVR01/thesis-literature"

    # Both in one pass
    python3 zotero_export.py --extra-dir "/mnt/c/Users/nils/Documents/academic/courses/lu/25VT/SPVR01/thesis-literature"
"""

import argparse
import csv
import hashlib
import os
import re
import shutil
import sqlite3
import sys
import tempfile
from pathlib import Path

INDEX_FIELDS = [
    "new_filename", "title", "author_first_listed", "year", "tags",
    "source", "zotero_key", "original_path",
]


def sanitize(s: str, max_len: int = 60) -> str:
    s = re.sub(r"[^\w\s-]", "", s, flags=re.UNICODE).strip()
    s = re.sub(r"\s+", "_", s)
    return s[:max_len] if s else "untitled"


def windows_safe_name(s: str, max_len: int = 150) -> str:
    """Lighter touch than sanitize(): strip only characters Windows can't
    handle in filenames, collapse whitespace, but otherwise preserve an
    already-reasonable (e.g. APA-ish) name."""
    s = re.sub(r'[<>:"/\\|?*\x00-\x1f]', "", s).strip()
    s = re.sub(r"\s+", " ", s)
    return s[:max_len] if s else "untitled"


def sha1_of(path: Path, chunk_size: int = 1 << 16) -> str:
    h = hashlib.sha1()
    with open(path, "rb") as f:
        while True:
            b = f.read(chunk_size)
            if not b:
                break
            h.update(b)
    return h.hexdigest()


def get_field_value(cur, item_id: int, field_name: str):
    cur.execute(
        """
        SELECT idv.value
        FROM itemData id
        JOIN fields f ON f.fieldID = id.fieldID
        JOIN itemDataValues idv ON idv.valueID = id.valueID
        WHERE id.itemID = ? AND f.fieldName = ?
        """,
        (item_id, field_name),
    )
    row = cur.fetchone()
    return row[0] if row else None


def get_year(cur, item_id: int):
    date_val = get_field_value(cur, item_id, "date")
    if not date_val:
        return None
    m = re.search(r"\d{4}", date_val)
    return m.group(0) if m else None


def get_first_author_lastname(cur, item_id: int):
    cur.execute(
        """
        SELECT c.lastName
        FROM itemCreators ic
        JOIN creators c ON c.creatorID = ic.creatorID
        WHERE ic.itemID = ?
        ORDER BY ic.orderIndex ASC
        LIMIT 1
        """,
        (item_id,),
    )
    row = cur.fetchone()
    return row[0] if row and row[0] else None


def get_tags(cur, item_id: int):
    cur.execute(
        """
        SELECT t.name FROM itemTags it
        JOIN tags t ON t.tagID = it.tagID
        WHERE it.itemID = ?
        """,
        (item_id,),
    )
    return "; ".join(r[0] for r in cur.fetchall())


def guess_zotero_dir() -> Path:
    """Best-effort default. Native Windows: %USERPROFILE%\\Zotero. Running under
    WSL against the Windows filesystem: probe /mnt/c/Users/*/Zotero. Falls back
    to ~/Zotero (a real Linux-native Zotero install) if nothing else matches."""
    userprofile = os.environ.get("USERPROFILE")
    if userprofile:
        return Path(userprofile) / "Zotero"

    mnt_users = Path("/mnt/c/Users")
    if mnt_users.exists():
        candidates = [p for p in mnt_users.glob("*/Zotero") if (p / "zotero.sqlite").exists()]
        if len(candidates) == 1:
            return candidates[0]
        if len(candidates) > 1:
            for c in candidates:
                if os.environ.get("USER", "") and os.environ["USER"] in str(c):
                    return c
            return candidates[0]

    return Path.home() / "Zotero"


def load_existing_index(out_dir: Path):
    """Returns (rows, used_names, existing_hashes). existing_hashes maps
    sha1 -> new_filename for every PDF currently sitting in out_dir, whether
    or not it's already listed in index.csv, so dedup works even if the CSV
    was hand-edited or deleted."""
    rows = []
    index_path = out_dir / "index.csv"
    if index_path.exists():
        with open(index_path, newline="", encoding="utf-8") as f:
            rows = list(csv.DictReader(f))

    used_names = {r["new_filename"] for r in rows}
    existing_hashes = {}
    if out_dir.exists():
        for p in out_dir.glob("*.pdf"):
            try:
                existing_hashes[sha1_of(p)] = p.name
            except OSError:
                pass
    return rows, used_names, existing_hashes


def unique_name(base_name: str, used_names: set, ext: str = ".pdf") -> str:
    new_name = f"{base_name}{ext}"
    n = 2
    while new_name in used_names:
        new_name = f"{base_name}_{n}{ext}"
        n += 1
    used_names.add(new_name)
    return new_name


def run_zotero_pass(args, rows, used_names, existing_hashes):
    db_path = args.zotero_dir / "zotero.sqlite"
    storage_dir = args.zotero_dir / "storage"

    if not db_path.exists():
        sys.exit(f"Can't find {db_path} - pass --zotero-dir if your Zotero data lives elsewhere.")
    if not storage_dir.exists():
        sys.exit(f"Can't find {storage_dir} - expected the Zotero storage folder next to zotero.sqlite.")

    tmp_db = Path(tempfile.gettempdir()) / "zotero_export_readonly.sqlite"
    try:
        shutil.copy2(db_path, tmp_db)
    except PermissionError:
        sys.exit(
            "Couldn't copy zotero.sqlite - it's likely still locked. "
            "Make sure Zotero is fully closed (check the system tray / task manager) and try again."
        )

    conn = sqlite3.connect(f"file:{tmp_db}?mode=ro", uri=True)
    cur = conn.cursor()

    cur.execute(
        """
        SELECT ia.itemID, ia.parentItemID, ia.path, i.key
        FROM itemAttachments ia
        JOIN items i ON i.itemID = ia.itemID
        WHERE ia.contentType = 'application/pdf'
          AND ia.linkMode IN (0, 1)
          AND ia.path LIKE 'storage:%'
        """
    )
    attachments = cur.fetchall()

    if not attachments:
        print("No stored PDF attachments found in this Zotero library.")
    copied = 0
    skipped_dupe = 0

    for item_id, parent_item_id, path, key in attachments:
        filename = path.split("storage:", 1)[1]
        src = storage_dir / key / filename
        if not src.exists():
            print(f"  [skip] missing file on disk: {src}")
            continue

        # Hash unconditionally (dry-run included) so the preview accurately
        # reports exact-duplicate skips instead of listing every path as new.
        digest = sha1_of(src)
        if digest in existing_hashes:
            skipped_dupe += 1
            if args.dry_run:
                print(f"  [dry-run/zotero] SKIP (duplicate of {existing_hashes[digest]}): {src.name}")
            continue

        meta_item_id = parent_item_id if parent_item_id else item_id
        title = get_field_value(cur, meta_item_id, "title") or Path(filename).stem
        year = get_year(cur, meta_item_id) or "n.d."
        author = get_first_author_lastname(cur, meta_item_id) or "Unknown"
        tags = get_tags(cur, meta_item_id)

        base_name = f"{sanitize(author, 30)}_{year}_{sanitize(title)}"
        new_name = unique_name(base_name, used_names)

        if args.dry_run:
            # Reserve the hash for this run only, so duplicates *within* this
            # same dry-run pass are still reported correctly.
            existing_hashes[digest] = new_name
            copied += 1
            print(f"  [dry-run/zotero] {src.name} -> {new_name}")
            continue

        dest = args.out_dir / new_name
        shutil.copy2(src, dest)
        existing_hashes[digest] = new_name
        rows.append({
            "new_filename": new_name, "title": title, "author_first_listed": author,
            "year": year, "tags": tags, "source": "zotero", "zotero_key": key,
            "original_path": str(src),
        })
        copied += 1
        print(f"  copied (zotero): {new_name}")

    conn.close()
    tmp_db.unlink(missing_ok=True)
    if skipped_dupe:
        print(f"  skipped {skipped_dupe} exact duplicate(s) already present in {args.out_dir}")
    return copied


def run_extra_dir_pass(extra_dir: Path, args, rows, used_names, existing_hashes):
    if not extra_dir.exists():
        print(f"  [warn] --extra-dir not found, skipping: {extra_dir}")
        return 0

    pdfs = sorted({p for p in extra_dir.rglob("*") if p.suffix.lower() == ".pdf"})
    copied = 0
    skipped_dupe = 0

    for src in pdfs:
        digest = sha1_of(src)
        if digest in existing_hashes:
            skipped_dupe += 1
            if args.dry_run:
                print(f"  [dry-run/manual] SKIP (duplicate of {existing_hashes[digest]}): {src}")
            continue

        title_guess = windows_safe_name(src.stem)
        new_name = unique_name(title_guess, used_names)

        if args.dry_run:
            existing_hashes[digest] = new_name
            copied += 1
            print(f"  [dry-run/manual] {src} -> {new_name}")
            continue

        dest = args.out_dir / new_name
        shutil.copy2(src, dest)
        existing_hashes[digest] = new_name
        rows.append({
            "new_filename": new_name, "title": title_guess, "author_first_listed": "",
            "year": "", "tags": "", "source": f"manual:{extra_dir}", "zotero_key": "",
            "original_path": str(src),
        })
        copied += 1
        print(f"  copied (manual): {new_name}")

    if skipped_dupe:
        print(f"  skipped {skipped_dupe} exact duplicate(s) from {extra_dir}")
    return copied


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    default_zotero_dir = guess_zotero_dir()
    default_out_dir = Path(__file__).resolve().parent.parent / "zotero-import"
    parser.add_argument("--zotero-dir", type=Path, default=default_zotero_dir,
                         help=f"Zotero data directory (default: {default_zotero_dir})")
    parser.add_argument("--out-dir", type=Path, default=default_out_dir,
                         help=f"Destination for copied PDFs + index (default: {default_out_dir})")
    parser.add_argument("--extra-dir", type=Path, action="append", default=[],
                         help="Additional folder to scan recursively for PDFs (repeatable)")
    parser.add_argument("--skip-zotero", action="store_true",
                         help="Skip the Zotero library pass (use with --extra-dir only)")
    parser.add_argument("--dry-run", action="store_true",
                         help="List what would be copied without copying anything")
    args = parser.parse_args()

    args.out_dir.mkdir(parents=True, exist_ok=True)
    rows, used_names, existing_hashes = load_existing_index(args.out_dir)

    total_copied = 0

    if not args.skip_zotero:
        total_copied += run_zotero_pass(args, rows, used_names, existing_hashes)

    for extra in args.extra_dir:
        total_copied += run_extra_dir_pass(extra, args, rows, used_names, existing_hashes)

    if args.dry_run:
        print(f"\nDry run: {total_copied} PDF(s) would be copied to {args.out_dir}")
        return

    index_path = args.out_dir / "index.csv"
    with open(index_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=INDEX_FIELDS)
        writer.writeheader()
        for r in rows:
            writer.writerow({k: r.get(k, "") for k in INDEX_FIELDS})

    print(f"\nDone: {total_copied} new PDF(s) copied this run, {len(rows)} total in {args.out_dir}")
    print(f"Index written to {index_path}")


if __name__ == "__main__":
    main()
