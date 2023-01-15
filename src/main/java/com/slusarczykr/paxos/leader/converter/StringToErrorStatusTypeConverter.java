package com.slusarczykr.paxos.leader.converter;

import com.slusarczykr.paxos.leader.discovery.state.ErrorStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToErrorStatusTypeConverter implements Converter<String, ErrorStatus.Type> {

    @Override
    public ErrorStatus.Type convert(String source) {
        try {
            return ErrorStatus.Type.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
