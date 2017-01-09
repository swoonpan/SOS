/*
 * Copyright (C) 2012-2017 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.decode.xml.stream.ogc.ows;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLStreamException;

import org.n52.shetland.ogc.gml.CodeType;
import org.n52.shetland.ogc.ows.exception.CodedException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.sos.aqd.AqdConstants;
import org.n52.sos.decode.xml.stream.XmlReader;

/**
 * TODO JavaDoc
 *
 * @author Christian Autermann
 */
public class CodeTypeReader extends XmlReader<CodeType> {

    private CodeType codeType;

    @Override
    protected void begin() throws XMLStreamException, CodedException {
        String codeSpace = attr(AqdConstants.AN_CODE_SPACE).orNull();
        try {
            this.codeType = new CodeType(chars(), new URI(codeSpace));
        } catch (URISyntaxException e) {
            throw new NoApplicableCodeException().causedBy(e).withMessage("Error while creating URI from '{}'", codeSpace);
        }
    }

    @Override
    protected CodeType finish() {
        return this.codeType;
    }

}
