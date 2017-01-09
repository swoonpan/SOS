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
package org.n52.sos.encode.streaming.sos.v2;

import java.io.OutputStream;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.apache.xmlbeans.XmlObject;

import org.n52.iceland.coding.CodingRepository;
import org.n52.iceland.coding.encode.XmlEncoderKey;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.Sos2StreamingConstants;
import org.n52.shetland.w3c.SchemaLocation;
import org.n52.shetland.w3c.W3CConstants;
import org.n52.sos.coding.encode.EncodingValues;
import org.n52.sos.coding.encode.ObservationEncoder;
import org.n52.sos.coding.encode.XmlStreamWriter;
import org.n52.sos.encode.streaming.StreamingDataEncoder;
import org.n52.sos.encode.streaming.StreamingEncoder;
import org.n52.sos.ogc.om.StreamingValue;
import org.n52.sos.response.GetObservationResponse;
import org.n52.sos.util.XmlOptionsHelper;
import org.n52.svalbard.SosHelperValues;
import org.n52.svalbard.encode.Encoder;
import org.n52.svalbard.encode.SchemaAwareEncoder;
import org.n52.svalbard.encode.exception.EncodingException;

import com.google.common.collect.Sets;

/**
 * Implementatio of {@link XmlStreamWriter} for {@link GetObservationResponse}
 *
 * @author <a href="mailto:c.hollmann@52north.org">Carsten Hollmann</a>
 * @since 4.1.0
 *
 */
public class GetObservationResponseXmlStreamWriter extends XmlStreamWriter<GetObservationResponse> implements StreamingDataEncoder {

    private GetObservationResponse response;

    /**
     * constructor
     */
    public GetObservationResponseXmlStreamWriter() {
    }

    /**
     * constructor
     *
     * @param response
     *            {@link GetObservationResponse} to write to stream
     */
    public GetObservationResponseXmlStreamWriter(GetObservationResponse response) {
        setResponse(response);
    }

    @Override
    public void write(OutputStream out) throws XMLStreamException, EncodingException {
        write(getResponse(), out);
    }

    @Override
    public void write(OutputStream out, EncodingValues encodingValues) throws XMLStreamException, EncodingException {
        write(getResponse(), out, encodingValues);
    }

    @Override
    public void write(GetObservationResponse response, OutputStream out) throws XMLStreamException, EncodingException {
        write(response, out, new EncodingValues());
    }

    @Override
    public void write(GetObservationResponse response, OutputStream out, EncodingValues encodingValues)
            throws XMLStreamException, EncodingException {
        try {
            init(out, encodingValues);
            start(encodingValues.isEmbedded());
            writeGetObservationResponseDoc(response, encodingValues);
            end();
            finish();
        } catch (XMLStreamException xmlse) {
            throw new EncodingException(xmlse);
        }
    }

    /**
     * Set the {@link GetObservationResponse} to be written to stream
     *
     * @param response
     *            {@link GetObservationResponse} to write to stream
     */
    protected void setResponse(GetObservationResponse response) {
        this.response = response;
    }

    /**
     * Get the {@link GetObservationResponse} to write to stream
     *
     * @return {@link GetObservationResponse} to write
     */
    protected GetObservationResponse getResponse() {
        return response;
    }

    private void writeGetObservationResponseDoc(GetObservationResponse response, EncodingValues encodingValues)
            throws XMLStreamException, EncodingException {
        start(Sos2StreamingConstants.GET_OBSERVATION_RESPONSE);
        namespace(W3CConstants.NS_XLINK_PREFIX, W3CConstants.NS_XLINK);
        namespace(Sos2StreamingConstants.NS_SOS_PREFIX, Sos2StreamingConstants.NS_SOS_20);
        // get observation encoder
        ObservationEncoder<XmlObject, OmObservation> encoder = findObservationEncoder(response.getResponseFormat());
        encodingValues.getAdditionalValues().with(SosHelperValues.DOCUMENT, null);
        encodingValues.setEncodingNamespace(response.getResponseFormat());
        // write schemaLocation
        schemaLocation(getSchemaLocation(encodingValues, encoder));
        writeNewLine();
        // Map<HelperValues, String> additionalValues = Maps.newHashMap();
        // additionalValues.put(HelperValues.DOCUMENT, null);
        // EncodingValues encodingValues = new
        // EncodingValues(additionalValues).setEncodingNamespace(response.getResponseFormat());
        if (!response.isSetMergeObservation()) {
            response.setMergeObservations(encoder.shouldObservationsWithSameXBeMerged());
        }
        for (OmObservation o : response.getObservationCollection()) {
           if (o.getValue() instanceof StreamingValue) {
                StreamingValue<?> streamingValue = (StreamingValue<?>) o.getValue();
                try {
                    if (streamingValue.hasNextValue()) {
                        if (response.isSetMergeObservation()) {
                            if (encoder.supportsResultStreamingForMergedValues()) {
                                writeObservationData(o, encoder, encodingValues);
                                writeNewLine();
                            } else {
                                for (OmObservation obs : streamingValue.mergeObservation()) {
                                    writeObservationData(obs, encoder, encodingValues);
                                    writeNewLine();
                                }
                            }
                        } else {
                            do {
                                writeObservationData(streamingValue.nextSingleObservation(), encoder, encodingValues);
                                writeNewLine();
                            } while (streamingValue.hasNextValue());
                        }
                    } else if (streamingValue.getValue() != null) {
                        writeObservationData(streamingValue.getValue().getValue(), encoder, encodingValues);
                        writeNewLine();
                    }
                } catch (OwsExceptionReport owse) {
                    throw new EncodingException(owse);
                }
            } else {
                writeObservationData(o, encoder, encodingValues);
                writeNewLine();
            }
        }
        indent--;
        end(Sos2StreamingConstants.GET_OBSERVATION_RESPONSE);
    }

    private Set<SchemaLocation> getSchemaLocation(EncodingValues encodingValue,
            ObservationEncoder<XmlObject, OmObservation> encoder) {
        Set<SchemaLocation> schemaLocations = Sets.newHashSet();


        if (encodingValue.isSetEncoder() && encodingValue.getEncoder() instanceof SchemaAwareEncoder) {
            schemaLocations.addAll(((SchemaAwareEncoder<?,?>) encodingValue.getEncoder()).getSchemaLocations());
        } else {
            schemaLocations.add(Sos2Constants.SOS_GET_OBSERVATION_SCHEMA_LOCATION);
        }
        if (encoder != null &&  encoder instanceof SchemaAwareEncoder) {
            schemaLocations.addAll(((SchemaAwareEncoder<?,?>) encoder).getSchemaLocations());
        }
        return schemaLocations;
    }

    @SuppressWarnings("unchecked")
    private void writeObservationData(OmObservation observation, ObservationEncoder<XmlObject, OmObservation> encoder,
            EncodingValues encodingValues) throws XMLStreamException, EncodingException {
        start(Sos2StreamingConstants.OBSERVATION_DATA);
        writeNewLine();
        if (encoder instanceof StreamingEncoder) {
            ((StreamingEncoder<XmlObject, OmObservation>) encoder).encode(observation, getOutputStream(),
                    encodingValues.setAsDocument(true).setEmbedded(true).setIndent(indent));
        } else {
            rawText(encoder.encode(observation, encodingValues.getAdditionalValues())
                    .xmlText(XmlOptionsHelper.getInstance().getXmlOptions()));
        }
        indent--;
        writeNewLine();
        end(Sos2StreamingConstants.OBSERVATION_DATA);
        indent++;
    }

    /**
     * Finds a O&Mv2 compatible {@link ObservationEncoder}
     *
     * @param responseFormat
     *            the response format
     *
     * @return the encoder or {@code null} if none is found
     *
     * @throws EncodingException
     *             if the found encoder is not a {@linkplain ObservationEncoder}
     */
    private ObservationEncoder<XmlObject, OmObservation> findObservationEncoder(String responseFormat)
            throws EncodingException {
        Encoder<XmlObject, OmObservation> encoder =
                CodingRepository.getInstance().getEncoder(new XmlEncoderKey(responseFormat, OmObservation.class));
        if (encoder == null) {
            return null;
        } else if (encoder instanceof ObservationEncoder) {
            ObservationEncoder<XmlObject, OmObservation> oe = (ObservationEncoder<XmlObject, OmObservation>) encoder;
            return oe.isObservationAndMeasurmentV20Type() ? oe : null;
        } else {
            throw new EncodingException("Error while encoding response, encoder is not of type ObservationEncoder!");
        }
    }

}
