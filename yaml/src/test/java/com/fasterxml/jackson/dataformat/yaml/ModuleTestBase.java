package com.fasterxml.jackson.dataformat.yaml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.fasterxml.jackson.core.*;

public abstract class ModuleTestBase extends junit.framework.TestCase {

    // Extracted inner class for test assertions
    protected static class TestAsserter {
        public static void assertToken(JsonToken expToken, JsonToken actToken) {
            if (actToken != expToken) {
                fail("Expected token "+expToken+", current token "+actToken);
            }
        }

        public static void assertType(Object ob, Class<?> expType) {
            if (ob == null) {
                fail("Expected an object of type "+expType.getName()+", got null");
            }
            Class<?> cls = ob.getClass();
            if (!expType.isAssignableFrom(cls)) {
                fail("Expected type "+expType.getName()+", got "+cls.getName());
            }
        }

        public static void assertYAML(String expOrig, String actOrig, String docMarker) {
            String exp = trimYAMLDocMarker(expOrig, docMarker).trim();
            String act = trimYAMLDocMarker(actOrig, docMarker).trim();
            if (!exp.equals(act)) {
                assertEquals(expOrig, actOrig);
            }
        }

        private static String trimYAMLDocMarker(String doc, String marker) {
            if (doc.startsWith(marker)) {
                doc = doc.substring(marker.length());
            }
            return doc.trim();
        }
    }

    // Extracted inner class for resource management
    protected static class ResourceManager {
        public static byte[] readResource(String ref, Class<?> contextClass) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final byte[] buf = new byte[4000];

            try (InputStream in = contextClass.getResourceAsStream(ref)) {
                if (in != null) {
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        bytes.write(buf, 0, len);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read resource '"+ref+"': "+e);
            }
            if (bytes.size() == 0) {
                throw new IllegalArgumentException("Failed to read resource '"+ref+"': empty resource?");
            }
            return bytes.toByteArray();
        }
    }

    // Moved to separate class but kept in same file for now
    protected static class FiveMinuteUser {
        public enum Gender { MALE, FEMALE }

        private Gender _gender;
        public String firstName, lastName;
        private boolean _isVerified;
        private byte[] _userImage;

        public FiveMinuteUser() { }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, byte[] data) {
            firstName = first;
            lastName = last;
            _isVerified = verified;
            _gender = g;
            _userImage = data;
        }

        // Getters/Setters
        public boolean isVerified() { return _isVerified; }
        public Gender getGender() { return _gender; }
        public byte[] getUserImage() { return _userImage; }
        public void setVerified(boolean b) { _isVerified = b; }
        public void setGender(Gender g) { _gender = g; }
        public void setUserImage(byte[] b) { _userImage = b; }

        // Refactored to use polymorphism instead of conditionals
        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (!(o instanceof FiveMinuteUser)) return false;

            FiveMinuteUser other = (FiveMinuteUser) o;
            return areBasicFieldsEqual(other) && areImagesEqual(other._userImage);
        }

        protected boolean areBasicFieldsEqual(FiveMinuteUser other) {
            return _isVerified == other._isVerified
                    && _gender == other._gender
                    && firstName.equals(other.firstName)
                    && lastName.equals(other.lastName);
        }

        protected boolean areImagesEqual(byte[] otherImage) {
            if (_userImage.length != otherImage.length) return false;
            for (int i = 0; i < _userImage.length; i++) {
                if (_userImage[i] != otherImage[i]) return false;
            }
            return true;
        }
    }

    // Constructor
    protected ModuleTestBase() { }

    // Factory methods
    protected YAMLFactoryBuilder streamFactoryBuilder() {
        return YAMLFactory.builder();
    }

    protected YAMLMapper newObjectMapper() {
        return mapperBuilder().build();
    }

    protected YAMLMapper.Builder mapperBuilder() {
        return YAMLMapper.builder();
    }

    protected YAMLMapper.Builder mapperBuilder(YAMLFactory f) {
        return YAMLMapper.builder(f);
    }

    // Base test helper methods that delegate to TestAsserter
    protected void assertToken(JsonToken expToken, JsonToken actToken) {
        TestAsserter.assertToken(expToken, actToken);
    }

    protected void assertToken(JsonToken expToken, JsonParser p) {
        assertToken(expToken, p.getCurrentToken());
    }

    protected void assertType(Object ob, Class<?> expType) {
        TestAsserter.assertType(ob, expType);
    }

    protected void assertYAML(String expOrig, String actOrig) {
        TestAsserter.assertYAML(expOrig, actOrig, "---");
    }

    // Utility methods
    public String quote(String str) {
        return '"'+str+'"';
    }

    public byte[] utf8(String str) {
        return str.getBytes(StandardCharsets.UTF_8);
    }

    protected String getAndVerifyText(JsonParser p) throws IOException {
        int actLen = p.getTextLength();
        char[] ch = p.getTextCharacters();
        String str2 = new String(ch, p.getTextOffset(), actLen);
        String str = p.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (p.token == "+p.getCurrentToken()+"): p.getText().length() ['"+str+"'] == "+str.length()+"; p.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);
        return str;
    }

    protected void verifyFieldName(JsonParser p, String expName) throws IOException {
        assertEquals(expName, p.getText());
        assertEquals(expName, p.currentName());
    }

    protected void verifyIntValue(JsonParser p, long expValue) throws IOException {
        assertEquals(String.valueOf(expValue), p.getText());
    }

    protected void verifyException(Throwable e, String... matches) {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    // Delegate resource reading to ResourceManager
    protected byte[] readResource(String ref) {
        return ResourceManager.readResource(ref, getClass());
    }
}