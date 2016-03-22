package com.syncapse.jenkinsci.plugins.awscloudformationwrapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.amazonaws.services.cloudformation.model.Parameter;
import hudson.util.IOUtils;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ParametersParserTest {

    private ParametersParser parametersParser = new ParametersParser();

    @Test
    public void testParse() throws Exception {
        String input = readFileAsString("ParametersParserTest.testParse.json");
        List<Parameter> parameters = parametersParser.parse(input);

        assertTrue(parameters.size() == 2);
        assertTrue(parameters.get(0).getParameterKey().equals("KeyPairName"));
        assertTrue(parameters.get(0).getParameterValue().equals("MyKey"));
        assertTrue(parameters.get(1).getParameterKey().equals("InstanceType"));
        assertTrue(parameters.get(1).getUsePreviousValue());
    }

    public String readFileAsString(String fileName) throws IOException {
        InputStream stream = ParametersParserTest.class.getResourceAsStream(fileName);
        String result;
        try {
            result = IOUtils.toString(stream);
        } finally {
            stream.close();
        }

        return result;
    }
}