package com.marcosdev7.financas.config;

import com.marcosdev7.financas.domain.Mes;
import org.springframework.core.convert.converter.Converter;

public class ConverterStringParaEnum implements Converter<String, Mes> {
    public Mes convert(String source){
    String string = source.toUpperCase();
    return Mes.valueOf(string);
    }
}
