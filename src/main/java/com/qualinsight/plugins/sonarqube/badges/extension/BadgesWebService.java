/*
 * qualinsight-plugins-sonarqube-badges
 * Copyright (c) 2015, QualInsight
 * http://www.qualinsight.com/
 *
 * This program is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program. If not, you can retrieve a copy
 * from <http://www.gnu.org/licenses/>.
 */
package com.qualinsight.plugins.sonarqube.badges.extension;

import java.io.InputStream;
import java.io.OutputStream;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.RequestHandler;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService;
import com.qualinsight.plugins.sonarqube.badges.internal.QualityGateStatus;
import com.qualinsight.plugins.sonarqube.badges.internal.QualityGateStatusRetriever;
import com.qualinsight.plugins.sonarqube.badges.internal.SVGImageGenerator;

/**
 * WebService extension that provides the plugins' webservice controller and action for generating SVG quality gate status images.
 *
 * @author Michel Pawlak
 */
public final class BadgesWebService implements WebService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BadgesWebService.class);

    private QualityGateStatusRetriever qualityGateStatusRetriever;

    private SVGImageGenerator svgImageGenerator;

    /**
     * {@link BadgesWebService} IoC constructor
     *
     * @param qualityGateStatusRetriever helper extension to retrieve quality gate status
     * @param svgImageGenerator helper extension to generate SVG images
     */
    public BadgesWebService(final QualityGateStatusRetriever qualityGateStatusRetriever, final SVGImageGenerator svgImageGenerator) {
        this.qualityGateStatusRetriever = qualityGateStatusRetriever;
        this.svgImageGenerator = svgImageGenerator;
    }

    @Override
    public void define(final Context context) {
        final NewController controller = context.createController("api/badges");
        controller.setDescription("SVG Badges generation web service");
        controller.createAction("gate")
            .setDescription("Retrieve the quality gate status of a project as a SVG image.")
            .setHandler(new RequestHandler() {

                @Override
                public void handle(final Request request, final Response response) throws Exception {
                    final String key = request.mandatoryParam("key");
                    LOGGER.debug("Retrieving quality gate status for key '{}'.", key);
                    final QualityGateStatus status = BadgesWebService.this.qualityGateStatusRetriever.retrieveFor(key);
                    // we prepare the response OutputStream
                    final OutputStream responseOutputStream = response.stream()
                        .setMediaType("image/svg+xml")
                        .output();
                    LOGGER.debug("Retrieving SVG image for for quality gate status '{}'.", status);
                    final InputStream svgImageInputStream = BadgesWebService.this.svgImageGenerator.svgImageInputStreamFor(status);
                    LOGGER.debug("Writing SVG image to response OutputStream.");
                    IOUtils.copy(svgImageInputStream, responseOutputStream);
                    responseOutputStream.close();
                    // don't close svgImageInputStream, we want it to be reusable
                }
            })
            .createParam("key")
            .setDescription("Key of the project")
            .setExampleValue("org.codehaus.sonar:sonar")
            .setRequired(true);
        controller.done();
    }

}
