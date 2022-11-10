package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import com.amazonaws.services.cloudformation.model.Parameter;
import hudson.util.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ParameterUtilsTest {

    @Test
    public void testParse() throws Exception {
        String input = readFileAsString("ParametersParserTest.testParse.json");
        List<Parameter> parameters = ParameterUtils.parse(input);

        assertTrue(parameters.size() == 2);
        assertTrue(parameters.get(0).getParameterKey().equals("KeyPairName"));
        assertTrue(parameters.get(0).getParameterValue().equals("MyKey"));
        assertTrue(parameters.get(1).getParameterKey().equals("InstanceType"));
        assertTrue(parameters.get(1).getUsePreviousValue());
    }

    @Test
    public void testConvert() throws Exception {
        HashMap<String, String> parametersMap = new LinkedHashMap<String, String>();
        parametersMap.put("key1", "value1");
        parametersMap.put("key2", "value2");
        List<Parameter> parameters = ParameterUtils.convert(parametersMap);

        assertTrue(parameters.size() == 2);
        assertTrue(parameters.get(0).getParameterKey().equals("key1"));
        assertTrue(parameters.get(0).getParameterValue().equals("value1"));
        assertTrue(parameters.get(1).getParameterKey().equals("key2"));
        assertTrue(parameters.get(1).getParameterValue().equals("value2"));
    }

    @Test
    public void testMerge() throws Exception {
        ArrayList<Parameter> first = new ArrayList<Parameter>();
        first.add(new Parameter().withParameterKey("key1").withParameterValue("value1"));
        first.add(new Parameter().withParameterKey("key2").withParameterValue("value2"));

        ArrayList<Parameter> second = new ArrayList<Parameter>();
        second.add(new Parameter().withParameterKey("key2").withParameterValue("value2"));
        second.add(new Parameter().withParameterKey("key3").withParameterValue("value3"));

        List<Parameter> parameters = ParameterUtils.merge(first, second);

        assertTrue(parameters.size() == 3);
        assertTrue(parameters.get(0).getParameterKey().equals("key1"));
        assertTrue(parameters.get(0).getParameterValue().equals("value1"));
        assertTrue(parameters.get(1).getParameterKey().equals("key2"));
        assertTrue(parameters.get(1).getParameterValue().equals("value2"));
        assertTrue(parameters.get(2).getParameterKey().equals("key3"));
        assertTrue(parameters.get(2).getParameterValue().equals("value3"));
    }

    public String readFileAsString(String fileName) throws IOException {
        InputStream stream = ParameterUtilsTest.class.getResourceAsStream(fileName);
        String result;
        try {
            result = IOUtils.toString(stream);
        } finally {
            stream.close();
        }

        return result;
    }
}