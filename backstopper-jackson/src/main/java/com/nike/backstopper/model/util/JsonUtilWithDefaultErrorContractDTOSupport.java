package com.nike.backstopper.model.util;

import com.nike.backstopper.model.DefaultErrorDTO;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Maps any Object to a JSON representation. The default {@link #writeValueAsString(Object)} method knows about
 * {@link DefaultErrorDTO} objects and will exclude empty {@link DefaultErrorDTO#metadata} maps from the serialized
 * output, and serialize {@link DefaultErrorDTO#code} as a JSON number rather than string when possible. It uses
 * {@link #DEFAULT_SMART_MAPPER} to do the serialization.
 *
 * <p>If you want to always output the metadata object and/or always output error codes as strings you can generate the
 * appropriate serializer by calling {@link #generateErrorContractObjectMapper(boolean, boolean)} and passing in
 * whatever arguments you need, then use that by calling {@link #writeValueAsString(Object, ObjectMapper)} and pass in
 * your custom serializer.
 *
 * <p>You can further define a default generic error response that will be returned if there's a problem during
 * serialization by calling {@link #writeValueAsString(Object, ObjectMapper, String)}. The other methods use
 * {@link #DEFAULT_ERROR_RESPONSE_STRING} as a default.
 * <p>
 * Created by dsand7 on 9/25/14.
 */
@SuppressWarnings("WeakerAccess")
public class JsonUtilWithDefaultErrorContractDTOSupport {

    private JsonUtilWithDefaultErrorContractDTOSupport() {
        // Do nothing
    }

    private static final Logger logger = LoggerFactory.getLogger(JsonUtilWithDefaultErrorContractDTOSupport.class);

    public static final ObjectMapper DEFAULT_SMART_MAPPER = generateErrorContractObjectMapper(true, true);
    public static final String DEFAULT_ERROR_RESPONSE_STRING =
        "{\"error_id\":\"%uuid%\",\"errors\":[{\"code\":10,\"message\":\"An error occurred while fulfilling the request\"}]}";

    public static String writeValueAsString(Object value) {
        return writeValueAsString(value, DEFAULT_SMART_MAPPER);
    }

    public static String writeValueAsString(Object value, ObjectMapper mapper) {
        return writeValueAsString(value, mapper, DEFAULT_ERROR_RESPONSE_STRING);
    }

    public static String writeValueAsString(Object value, ObjectMapper mapper,
                                            String defaultResponseIfErrorDuringSerialization) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            String errorId = UUID.randomUUID().toString();
            logger.error("Exception occurred while generating error code JSON. Falling back to default response with "
                         + "error_id={}", errorId, e);
            if (defaultResponseIfErrorDuringSerialization == null) {
                defaultResponseIfErrorDuringSerialization = DEFAULT_ERROR_RESPONSE_STRING;
            }
            return defaultResponseIfErrorDuringSerialization.replace("%uuid%", errorId);
        }
    }

    public static ObjectMapper generateErrorContractObjectMapper(boolean excludeEmptyMetadataFromJson,
                                                                 boolean serializeErrorCodeFieldAsIntegerIfPossible) {
        return new ObjectMapper().setSerializerFactory(
            new ErrorContractSerializationFactory(null, excludeEmptyMetadataFromJson,
                                                  serializeErrorCodeFieldAsIntegerIfPossible)
        );
    }

    protected static class ErrorContractSerializationFactory extends BeanSerializerFactory {

        private static final String METADATA_FIELD_NAME = "metadata";
        private static final String ERROR_CODE_FIELD_NAME = "code";

        private final boolean excludeEmptyMetadataFromJson;
        private final boolean serializeErrorCodeFieldAsIntegerIfPossible;

        protected ErrorContractSerializationFactory(SerializerFactoryConfig config,
                                                    boolean excludeEmptyMetadataFromJson,
                                                    boolean serializeErrorCodeFieldAsIntegerIfPossible) {
            super(config);
            this.excludeEmptyMetadataFromJson = excludeEmptyMetadataFromJson;
            this.serializeErrorCodeFieldAsIntegerIfPossible = serializeErrorCodeFieldAsIntegerIfPossible;
        }

        @Override
        protected List<BeanPropertyWriter> filterBeanProperties(SerializationConfig config, BeanDescription beanDesc,
                                                                List<BeanPropertyWriter> props) {
            List<BeanPropertyWriter> superResult = super.filterBeanProperties(config, beanDesc, props);

            if (DefaultErrorDTO.class.equals(beanDesc.getBeanClass())) {
                // Filter out empty metadata if desired
                if (excludeEmptyMetadataFromJson) {
                    BeanPropertyWriter origMetadataPropWriter = findPropWriter(superResult, METADATA_FIELD_NAME);
                    int indexOfOrig = superResult.indexOf(origMetadataPropWriter);
                    superResult.remove(origMetadataPropWriter);
                    superResult.add(indexOfOrig, new MetadataPropertyWriter(origMetadataPropWriter));
                }

                // Add a smart error code writer if desired
                if (serializeErrorCodeFieldAsIntegerIfPossible) {
                    BeanPropertyWriter origErrorCodePropWriter = findPropWriter(superResult, ERROR_CODE_FIELD_NAME);
                    int indexOfOrig = superResult.indexOf(origErrorCodePropWriter);
                    superResult.remove(origErrorCodePropWriter);
                    superResult.add(indexOfOrig, new SmartErrorCodePropertyWriter(origErrorCodePropWriter));
                }
            }

            return superResult;
        }

        protected BeanPropertyWriter findPropWriter(List<BeanPropertyWriter> propWriters, String desiredFieldName) {
            for (BeanPropertyWriter propWriter : propWriters) {
                if (desiredFieldName.equals(propWriter.getName())) {
                    return propWriter;
                }
            }

            return null;
        }
    }

    protected static class MetadataPropertyWriter extends BeanPropertyWriter {

        protected MetadataPropertyWriter(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception {
            if (bean instanceof DefaultErrorDTO error) {
                if (error.metadata == null || error.metadata.isEmpty()) {
                    return; // empty metadata. Don't serialize
                }
            }
            super.serializeAsField(bean, jgen, prov);
        }
    }

    protected static class SmartErrorCodePropertyWriter extends BeanPropertyWriter {

        protected SmartErrorCodePropertyWriter(BeanPropertyWriter base) {
            super(base);
        }

        @Override
        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov) throws Exception {
            if (bean instanceof DefaultErrorDTO error) {
                try {
                    int codeAsInt = Integer.parseInt(error.code);
                    jgen.writeFieldName(_name);
                    jgen.writeNumber(codeAsInt);
                    return;
                } catch (Throwable t) {
                    // Do nothing - let it be serialized normally as a string.
                }
            }
            super.serializeAsField(bean, jgen, prov);
        }
    }
}
