package io.github.nilsfjp.ideophonearena.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StimulusResourceConfig implements WebMvcConfigurer {

    private final String[] stimulusLocations;

    public StimulusResourceConfig(
            @Value("${app.stimuli.locations:classpath:/static/stimuli/,file:/code/js/ideophone-arena-web/dist/stimuli/}")
            String[] stimulusLocations) {
        this.stimulusLocations = stimulusLocations;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/stimuli/**")
                .addResourceLocations(stimulusLocations);
    }
}
