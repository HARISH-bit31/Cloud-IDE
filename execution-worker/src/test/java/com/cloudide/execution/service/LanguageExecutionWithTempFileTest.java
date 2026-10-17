package com.cloudide.execution.service;

import com.cloudide.execution.config.ExecutionWorkerProperties;
import com.cloudide.execution.dto.ExecutionResponse;
import com.cloudide.execution.dto.ExecutionStartRequest;
import com.cloudide.execution.enums.ExecutionStatus;
import com.cloudide.execution.enums.ProgrammingLanguage;
import org.junit.jupiter.api.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LanguageExecutionWithTempFileTest {

    private static DockerSandboxService service;
    private static ExecutionWorkerProperties props;

    @BeforeAll
    static void setUp() {
        props = new ExecutionWorkerProperties();
        service = new DockerSandboxService(props);
    }

    @AfterAll
    static void tearDown() {
        if (service != null) {
            service.cleanupAll();
        }
    }

    private ExecutionResponse awaitCompletion(String executionId, int timeoutSeconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000L);
        ExecutionResponse response = null;
        while (System.currentTimeMillis() < deadline) {
            response = service.getExecutionStatus(executionId);
            if (response != null && response.getStatus() != ExecutionStatus.RUNNING
                    && response.getStatus() != ExecutionStatus.WAITING_FOR_INPUT) {
                return response;
            }
            Thread.sleep(100);
        }
        return response;
    }

    @Test
    @Order(1)
    @DisplayName("Python execution with temporary file access in /tmp succeeds")
    void testPythonWithTempFile() throws Exception {
        Assumptions.assumeTrue(service.isDockerAvailable(), "Docker daemon not available, skipping live container test");

        String execId = UUID.randomUUID().toString();
        String code = "import tempfile\n" +
                "with open('/tmp/pytemp.txt', 'w') as f:\n" +
                "    f.write('py-temp-ok')\n" +
                "print(open('/tmp/pytemp.txt').read().strip())";

        ExecutionStartRequest req = new ExecutionStartRequest(execId, ProgrammingLanguage.PYTHON, code, null, 10);
        ExecutionResponse startRes = service.startExecution(req);
        assertNotNull(startRes);

        ExecutionResponse finalRes = awaitCompletion(execId, 10);
        assertNotNull(finalRes);
        assertEquals(ExecutionStatus.SUCCESS, finalRes.getStatus(), "Python run should succeed: " + finalRes.getStderr());
        assertTrue(finalRes.getStdout().contains("py-temp-ok"));
    }

    @Test
    @Order(2)
    @DisplayName("Read-only root filesystem blocks writes to / and protects container")
    void testReadOnlyRootProtection() throws Exception {
        Assumptions.assumeTrue(service.isDockerAvailable(), "Docker daemon not available, skipping live container test");

        String execId = UUID.randomUUID().toString();
        String code = "try:\n" +
                "    with open('/blocked.txt', 'w') as f:\n" +
                "        f.write('fail')\n" +
                "    print('VULNERABLE')\n" +
                "except OSError:\n" +
                "    print('PROTECTED_READ_ONLY')";

        ExecutionStartRequest req = new ExecutionStartRequest(execId, ProgrammingLanguage.PYTHON, code, null, 10);
        service.startExecution(req);

        ExecutionResponse finalRes = awaitCompletion(execId, 10);
        assertNotNull(finalRes);
        assertEquals(ExecutionStatus.SUCCESS, finalRes.getStatus());
        assertTrue(finalRes.getStdout().contains("PROTECTED_READ_ONLY"), "Root filesystem write must be blocked");
        assertFalse(finalRes.getStdout().contains("VULNERABLE"));
    }

    @Test
    @Order(3)
    @DisplayName("Java execution with temporary file access in /tmp succeeds")
    void testJavaWithTempFile() throws Exception {
        Assumptions.assumeTrue(service.isDockerAvailable(), "Docker daemon not available, skipping live container test");

        String execId = UUID.randomUUID().toString();
        String code = "public class Main {\n" +
                "    public static void main(String[] args) throws Exception {\n" +
                "        java.io.File temp = java.io.File.createTempFile(\"javatemp\", \".txt\");\n" +
                "        java.nio.file.Files.writeString(temp.toPath(), \"java-temp-ok\");\n" +
                "        System.out.println(java.nio.file.Files.readString(temp.toPath()));\n" +
                "    }\n" +
                "}";

        ExecutionStartRequest req = new ExecutionStartRequest(execId, ProgrammingLanguage.JAVA, code, null, 10);
        service.startExecution(req);

        ExecutionResponse finalRes = awaitCompletion(execId, 10);
        assertNotNull(finalRes);
        assertEquals(ExecutionStatus.SUCCESS, finalRes.getStatus(), "Java run should succeed: " + finalRes.getStderr());
        assertTrue(finalRes.getStdout().contains("java-temp-ok"));
    }

    @Test
    @Order(4)
    @DisplayName("C execution with temporary file access in /tmp succeeds")
    void testCWithTempFile() throws Exception {
        Assumptions.assumeTrue(service.isDockerAvailable(), "Docker daemon not available, skipping live container test");

        String execId = UUID.randomUUID().toString();
        String code = "#include <stdio.h>\n" +
                "int main() {\n" +
                "    FILE *f = fopen(\"/tmp/ctemp.txt\", \"w\");\n" +
                "    if (f) {\n" +
                "        fputs(\"c-temp-ok\\n\", f);\n" +
                "        fclose(f);\n" +
                "    }\n" +
                "    FILE *r = fopen(\"/tmp/ctemp.txt\", \"r\");\n" +
                "    char buf[32];\n" +
                "    if (r && fgets(buf, sizeof(buf), r)) {\n" +
                "        printf(\"%s\", buf);\n" +
                "        fclose(r);\n" +
                "    }\n" +
                "    return 0;\n" +
                "}";

        ExecutionStartRequest req = new ExecutionStartRequest(execId, ProgrammingLanguage.C, code, null, 10);
        service.startExecution(req);

        ExecutionResponse finalRes = awaitCompletion(execId, 10);
        assertNotNull(finalRes);
        assertEquals(ExecutionStatus.SUCCESS, finalRes.getStatus(), "C run should succeed: " + finalRes.getStderr());
        assertTrue(finalRes.getStdout().contains("c-temp-ok"));
    }

    @Test
    @Order(5)
    @DisplayName("C++ execution with temporary file access in /tmp succeeds")
    void testCppWithTempFile() throws Exception {
        Assumptions.assumeTrue(service.isDockerAvailable(), "Docker daemon not available, skipping live container test");

        String execId = UUID.randomUUID().toString();
        String code = "#include <iostream>\n" +
                "#include <fstream>\n" +
                "#include <string>\n" +
                "int main() {\n" +
                "    std::ofstream out(\"/tmp/cpptemp.txt\");\n" +
                "    out << \"cpp-temp-ok\";\n" +
                "    out.close();\n" +
                "    std::ifstream in(\"/tmp/cpptemp.txt\");\n" +
                "    std::string s;\n" +
                "    in >> s;\n" +
                "    std::cout << s << std::endl;\n" +
                "    return 0;\n" +
                "}";

        ExecutionStartRequest req = new ExecutionStartRequest(execId, ProgrammingLanguage.CPP, code, null, 10);
        service.startExecution(req);

        ExecutionResponse finalRes = awaitCompletion(execId, 10);
        assertNotNull(finalRes);
        assertEquals(ExecutionStatus.SUCCESS, finalRes.getStatus(), "C++ run should succeed: " + finalRes.getStderr());
        assertTrue(finalRes.getStdout().contains("cpp-temp-ok"));
    }
}
