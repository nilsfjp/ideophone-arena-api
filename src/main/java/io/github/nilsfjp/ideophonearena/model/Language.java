package io.github.nilsfjp.ideophonearena.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// A first-class language (ADR-1). Every v1 word backfills to jpn; cross-linguistic
// languages arrive with XL ingestion. name/family/player_note are player-facing copy.
@Entity
@Table(name = "languages")
public class Language {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "iso_code", nullable = false, unique = true, length = 8)
    private String isoCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 50)
    private String family;

    @Column(name = "player_note", length = 255)
    private String playerNote;

    protected Language() {
    }

    public Language(String isoCode, String name, String family, String playerNote) {
        this.isoCode = isoCode;
        this.name = name;
        this.family = family;
        this.playerNote = playerNote;
    }

    public Long getId() {
        return id;
    }

    public String getIsoCode() {
        return isoCode;
    }

    public String getName() {
        return name;
    }

    public String getFamily() {
        return family;
    }

    public String getPlayerNote() {
        return playerNote;
    }
}
