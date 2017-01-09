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
package org.n52.sos.ds.hibernate.util.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.n52.shetland.ogc.om.MultiObservationValues;
import org.n52.shetland.ogc.om.ObservationValue;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.OmObservationConstellation;
import org.n52.shetland.ogc.om.SingleObservationValue;
import org.n52.shetland.ogc.om.values.BooleanValue;
import org.n52.shetland.ogc.om.values.CategoryValue;
import org.n52.shetland.ogc.om.values.ComplexValue;
import org.n52.shetland.ogc.om.values.CountValue;
import org.n52.shetland.ogc.om.values.QuantityValue;
import org.n52.shetland.ogc.om.values.SweDataArrayValue;
import org.n52.shetland.ogc.om.values.TextValue;
import org.n52.shetland.ogc.om.values.Value;
import org.n52.shetland.ogc.ows.exception.CodedException;
import org.n52.shetland.ogc.ows.exception.NoApplicableCodeException;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.swe.SweAbstractDataComponent;
import org.n52.shetland.ogc.swe.SweDataRecord;
import org.n52.shetland.ogc.swe.SweField;
import org.n52.shetland.ogc.swe.simpleType.SweAbstractSimpleType;
import org.n52.shetland.ogc.swe.simpleType.SweBoolean;
import org.n52.shetland.ogc.swe.simpleType.SweCategory;
import org.n52.shetland.ogc.swe.simpleType.SweCount;
import org.n52.shetland.ogc.swe.simpleType.SweQuantity;
import org.n52.shetland.ogc.swe.simpleType.SweText;
import org.n52.shetland.ogc.swe.simpleType.SweTime;
import org.n52.shetland.ogc.swe.simpleType.SweTimeRange;
import org.n52.shetland.util.DateTimeHelper;
import org.n52.shetland.util.DateTimeParseException;

import com.google.common.collect.Maps;

/**
 * TODO JavaDoc
 * @author Christian Autermann <c.autermann@52north.org>
 */
public class ObservationUnfolder {
    private final OmObservation multiObservation;

    public ObservationUnfolder(OmObservation multiObservation) {
        this.multiObservation = multiObservation;
    }

    public List<OmObservation> unfold() throws OwsExceptionReport {
        if (multiObservation.getValue() instanceof SingleObservationValue) {
            return Collections.singletonList(multiObservation);
        } else {
            final List<OmObservation> observationCollection = new ArrayList<>();
            if (((MultiObservationValues<?>) multiObservation.getValue()).getValue() instanceof SweDataArrayValue) {
                final SweDataArrayValue arrayValue =
                        (SweDataArrayValue) ((MultiObservationValues<?>) multiObservation.getValue()).getValue();
                final List<List<String>> values = arrayValue.getValue().getValues();
                SweDataRecord elementType = null;
                if (arrayValue.getValue().getElementType() != null
                        && arrayValue.getValue().getElementType() instanceof SweDataRecord) {
                    elementType = (SweDataRecord) arrayValue.getValue().getElementType();
                } else {
                    throw new NoApplicableCodeException().withMessage("sweElementType type \"%s\" not supported",
                            elementType != null ? elementType.getClass().getName() : "null");
                }

                for (final List<String> block : values) {
                    int tokenIndex = 0;
                    Time phenomenonTime = null;
                    TimeInstant resultTime = null;
                    final List<Value<?>> observedValues = new LinkedList<>();
                    // map to store the observed properties
                    final Map<Value<?>, String> definitionsForObservedValues = Maps.newHashMap();
                    Value<?> observedValue = null;

                    for (SweField field : elementType.getFields()) {
                        final SweAbstractDataComponent dataComponent =  field.getElement();
                        String token = block.get(tokenIndex);
                        /*
                         * get phenomenon time
                         */
                        if (dataComponent instanceof SweTime) {
                            try {
                                if (dataComponent.isSetDefinition()
                                        && OmConstants.RESULT_TIME.equals(dataComponent.getDefinition())) {
                                    resultTime = new TimeInstant(DateTimeHelper.parseIsoString2DateTime(token));
                                } else {
                                    if (phenomenonTime == null) {
                                        phenomenonTime = new TimeInstant(DateTimeHelper.parseIsoString2DateTime(token));
                                    }
                                }
                            } catch (DateTimeParseException e) {
                                /*
                                 * FIXME what is the valid exception code if the
                                 * result is not correct?
                                 */
                                throw new NoApplicableCodeException().causedBy(e)
                                        .withMessage("Error while parse time String to DateTime!");
                            }
                        } else if (dataComponent instanceof SweTimeRange) {
                            try {
                                final String[] subTokens = token.split("/");
                                phenomenonTime = new TimePeriod(DateTimeHelper.parseIsoString2DateTime(subTokens[0]),
                                        DateTimeHelper.parseIsoString2DateTime(subTokens[1]));
                            } catch (DateTimeParseException e) {
                                /*
                                 * FIXME what is the valid exception code if the
                                 * result is not correct?
                                 */
                                throw new NoApplicableCodeException().causedBy(e)
                                        .withMessage("Error while parse time String to DateTime!");
                            }
                        }
                        /*
                         * observation values
                         */
                        else if (dataComponent instanceof SweAbstractSimpleType) {
                            observedValue = parseSweAbstractSimpleType(dataComponent, token);
                        } else if (dataComponent instanceof SweDataRecord) {
                            try {
                                observedValue =
                                        parseSweDataRecord(((SweDataRecord) dataComponent).clone(), block, tokenIndex);
                            } catch (CloneNotSupportedException e) {
                                throw new NoApplicableCodeException().causedBy(e).withMessage(
                                        "Unable to copy element '%s'.", dataComponent.getClass().getName());
                            }
                        } else {
                            throw new NoApplicableCodeException().withMessage("sweField type '%s' not supported",
                                    dataComponent != null ? dataComponent.getClass().getName() : "null");
                        }
                        if (observedValue != null) {
                            definitionsForObservedValues.put(observedValue, dataComponent.getDefinition());
                            observedValues.add(observedValue);
                            observedValue = null;
                        }
                        tokenIndex++;
                    }
                    for (final Value<?> iValue : observedValues) {
                        final OmObservation newObservation =
                                createSingleValueObservation(multiObservation, phenomenonTime, resultTime, iValue);
                        observationCollection.add(newObservation);
                    }
                }
            }
            return observationCollection;
        }
    }

    private Value<?> parseSweAbstractSimpleType(SweAbstractDataComponent dataComponent, String token) throws CodedException {
        Value<?> observedValue = null;
        if (dataComponent instanceof SweQuantity) {
            observedValue = new QuantityValue(Double.parseDouble(token));
            observedValue.setUnit(((SweQuantity) dataComponent).getUom());
        } else if (dataComponent instanceof SweBoolean) {
            observedValue = new BooleanValue(Boolean.parseBoolean(token));
        } else if (dataComponent instanceof SweText) {
            observedValue = new TextValue(token);
        } else if (dataComponent instanceof SweCategory) {
            observedValue = new CategoryValue(token);
            observedValue.setUnit(((SweCategory) dataComponent).getCodeSpace());
        } else if (dataComponent instanceof SweCount) {
            observedValue = new CountValue(Integer.parseInt(token));
        } else {
            throw new NoApplicableCodeException().withMessage("sweField type '%s' not supported",
                    dataComponent != null ? dataComponent.getClass().getName() : "null");
        }
        return observedValue;
    }

    private Value<?> parseSweDataRecord(SweDataRecord record, List<String> block, int tokenIndex) throws CodedException {
        for (SweField field : record.getFields()) {
            String token = block.get(tokenIndex);
            if (field.getElement() instanceof SweQuantity) {
                ((SweQuantity)field.getElement()).setValue(Double.parseDouble(token));
            } else if (field.getElement() instanceof SweBoolean) {
                ((SweBoolean)field.getElement()).setValue(Boolean.parseBoolean(token));
            } else if (field.getElement() instanceof SweText) {
                ((SweText)field.getElement()).setValue(token);
            } else if (field.getElement() instanceof SweCategory) {
                ((SweCategory)field.getElement()).setValue(token);
            } else if (field.getElement() instanceof SweCount) {
                ((SweCount)field.getElement()).setValue(Integer.parseInt(token));
            } else {
                throw new NoApplicableCodeException().withMessage("sweField type '%s' not supported",
                                                                  field.getClass().getName());
            }
        }
        return new ComplexValue(record);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private OmObservation createSingleValueObservation(final OmObservation multiObservation,
            final Time phenomenonTime, TimeInstant resultTime, final Value<?> iValue) {
        final ObservationValue<?> value = new SingleObservationValue(phenomenonTime, iValue);
        final OmObservation newObservation = new OmObservation();
        newObservation.setNoDataValue(multiObservation.getNoDataValue());
        /*
         * TODO create new ObservationConstellation only with the specified
         * observed property and observation type
         */
        final OmObservationConstellation obsConst = multiObservation.getObservationConstellation();
        /*
         * createObservationConstellationForSubObservation ( multiObservation .
         * getObservationConstellation ( ) , iValue ,
         * definitionsForObservedValues . get ( iValue ) )
         */
        newObservation.setObservationConstellation(obsConst);
        newObservation.setValidTime(multiObservation.getValidTime());
        if (resultTime != null && !resultTime.isEmpty()) {
            newObservation.setResultTime(resultTime);
        } else if (multiObservation.isSetResultTime() && !multiObservation.getResultTime().isEmpty()) {
            newObservation.setResultTime(multiObservation.getResultTime());
        } else {
            if (phenomenonTime instanceof TimeInstant) {
                newObservation.setResultTime((TimeInstant)phenomenonTime);
            } else if (phenomenonTime instanceof TimePeriod) {
                newObservation.setResultTime(new TimeInstant(((TimePeriod)phenomenonTime).getEnd()));
            }
        }
        newObservation.setTokenSeparator(multiObservation.getTokenSeparator());
        newObservation.setTupleSeparator(multiObservation.getTupleSeparator());
        newObservation.setDecimalSeparator(multiObservation.getDecimalSeparator());
        newObservation.setResultType(multiObservation.getResultType());
        newObservation.setValue(value);
        return newObservation;
    }

}
