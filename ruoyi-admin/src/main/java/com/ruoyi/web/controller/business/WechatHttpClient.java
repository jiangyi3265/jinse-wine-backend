package com.ruoyi.web.controller.business;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/** WeChat JSON responses may be labelled text/plain, including token errors. */
final class WechatHttpClient
{
    private WechatHttpClient() { }

    static RestTemplate create()
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        RestTemplate client = new RestTemplate(factory);
        for (HttpMessageConverter<?> converter : client.getMessageConverters())
        {
            if (converter instanceof MappingJackson2HttpMessageConverter)
            {
                MappingJackson2HttpMessageConverter jackson = (MappingJackson2HttpMessageConverter) converter;
                List<MediaType> types = new ArrayList<>(jackson.getSupportedMediaTypes());
                types.add(MediaType.TEXT_PLAIN);
                jackson.setSupportedMediaTypes(types);
            }
        }
        return client;
    }
}
