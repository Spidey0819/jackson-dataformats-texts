package com.fasterxml.jackson.dataformat.yaml.misc;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.yaml.ModuleTestBase;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class ObjectIdTest extends ModuleTestBase {
    // Extracted into nested static class for test data
    static class TestData {
        static final String SIMPLE_YAML_NATIVE =
                "---\n"
                        +"&1 name: \"first\"\n"
                        +"next:\n"
                        +"  &2 name: \"second\"\n"
                        +"  next: *1";

        static final String SIMPLE_YAML_NATIVE_B =
                "---\n"
                        +"&id1 name: \"first\"\n"
                        +"next:\n"
                        +"  &id2 name: \"second\"\n"
                        +"  next: *id1";

        static final String SIMPLE_YAML_NON_NATIVE =
                "---\n"
                        +"'@id': 1\n"
                        +"name: \"first\"\n"
                        +"next:\n"
                        +"  '@id': 2\n"
                        +"  name: \"second\"\n"
                        +"  next: 1";
    }

    // Pull up common ID generation functionality
    static abstract class BaseIdGenerator<T> extends ObjectIdGenerator<T> {
        private static final long serialVersionUID = 1L;
        protected final Class<?> scope;

        protected BaseIdGenerator(Class<?> scope) {
            this.scope = scope;
        }

        @Override
        public final Class<?> getScope() {
            return scope;
        }

        @Override
        public boolean canUseFor(ObjectIdGenerator<?> gen) {
            return (gen.getClass() == getClass()) && (gen.getScope() == scope);
        }

        protected abstract ObjectIdGenerator<T> createForScope(Class<?> scope);
    }

    // Refactored to extend BaseIdGenerator
    static class PrefixIdGenerator extends BaseIdGenerator<String> {
        private static final long serialVersionUID = 1L;
        protected transient int nextValue;

        protected PrefixIdGenerator() {
            this(Object.class);
        }

        protected PrefixIdGenerator(Class<?> scope) {
            super(scope);
        }

        @Override
        public String generateId(Object forPojo) {
            return "id" + (nextValue++);
        }

        @Override
        protected ObjectIdGenerator<String> createForScope(Class<?> scope) {
            return new PrefixIdGenerator(scope);
        }

        @Override
        public ObjectIdGenerator<String> forScope(Class<?> scope) {
            return (this.scope == scope) ? this : new PrefixIdGenerator(scope);
        }

        @Override
        public ObjectIdGenerator<String> newForSerialization(Object context) {
            return new PrefixIdGenerator(scope);
        }

        @Override
        public IdKey key(Object key) {
            return new ObjectIdGenerator.IdKey(getClass(), scope, key);
        }
    }

    // Extracted node verification logic
    static class NodeVerifier {
        static void verify(Node n) {
            assertNotNull(n);
            assertEquals("first", n.name);
            assertNotNull(n.next);
            assertEquals("second", n.next.name);
            assertNotNull(n.next.next);
            assertSame(n, n.next.next);
        }
    }

    @JsonIdentityInfo(generator=ObjectIdGenerators.IntSequenceGenerator.class, property="@id")
    @JsonPropertyOrder({ "name", "next"})
    static class Node {
        public String name;
        public Node next;

        public Node() { }
        public Node(String name) {
            this.name = name;
        }
    }

    @JsonIdentityInfo(generator=PrefixIdGenerator.class)
    static class NodeWithStringId {
        public String name;
        public NodeWithStringId next;

        public NodeWithStringId() { }
        public NodeWithStringId(String name) {
            this.name = name;
        }
    }

    private final ObjectMapper MAPPER = newObjectMapper();

    public void testNativeSerialization() throws Exception {
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = MAPPER.writeValueAsString(first);
        assertYAML(TestData.SIMPLE_YAML_NATIVE, yaml);
    }

    public void testNonNativeSerialization() throws Exception {
        YAMLMapper mapper = YAMLMapper.builder()
                .disable(YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID)
                .build();
        Node first = new Node("first");
        Node second = new Node("second");
        first.next = second;
        second.next = first;
        String yaml = mapper.writeValueAsString(first);
        assertYAML(TestData.SIMPLE_YAML_NON_NATIVE, yaml);
    }

    public void testBasicDeserialization() throws Exception {
        Node first = MAPPER.readValue(TestData.SIMPLE_YAML_NATIVE, Node.class);
        NodeVerifier.verify(first);

        Node second = MAPPER.readValue(TestData.SIMPLE_YAML_NON_NATIVE, Node.class);
        NodeVerifier.verify(second);
    }

    public void testDeserializationIssue45() throws Exception {
        NodeWithStringId node = MAPPER.readValue(TestData.SIMPLE_YAML_NATIVE_B, NodeWithStringId.class);
        assertNotNull(node);
        assertEquals("first", node.name);
        assertNotNull(node.next);
        assertEquals("second", node.next.name);
        assertNotNull(node.next.next);
        assertSame(node, node.next.next);
    }

    public void testRoundtripWithBuffer() throws Exception {
        TokenBuffer tbuf = MAPPER.readValue(TestData.SIMPLE_YAML_NATIVE, TokenBuffer.class);
        assertNotNull(tbuf);
        Node first = MAPPER.readValue(tbuf.asParser(), Node.class);
        tbuf.close();
        assertNotNull(first);
        NodeVerifier.verify(first);
    }
}