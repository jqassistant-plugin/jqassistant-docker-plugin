package org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.Providers;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.jqassistant.contrib.plugin.docker.impl.scanner.registry.client.model.Manifest.MEDIA_TYPE;

@Provider
@Consumes(MEDIA_TYPE)
public class ManifestMessageBodyReader implements MessageBodyReader<Manifest> {

    private final Providers providers;

    public ManifestMessageBodyReader(@Context Providers providers) {
        this.providers = providers;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return true;
    }

    @Override
    public Manifest readFrom(Class<Manifest> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                             MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
        throws IOException {
        MessageBodyReader<Manifest> delegate = providers.getMessageBodyReader(type, genericType, annotations,
            APPLICATION_JSON_TYPE);
        return delegate.readFrom(type, genericType, annotations, APPLICATION_JSON_TYPE, httpHeaders, entityStream);
    }

}
