package com.cloudide.execution.service;

import com.cloudide.execution.config.ExecutionWorkerProperties;
import com.cloudide.execution.enums.ProgrammingLanguage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DockerSandboxSecurityOptionsTest {

    @Test
    @DisplayName("Container run command includes cap-drop ALL, no-new-privileges, read-only root, and tmpfs /tmp")
    void testHardenedContainerOptionsIncluded(@TempDir Path tempDir) {
        ExecutionWorkerProperties props = new ExecutionWorkerProperties();
        DockerSandboxService service = new DockerSandboxService(props);

        List<String> command = service.buildDockerRunCommand("cloudide-test-container", tempDir, ProgrammingLanguage.PYTHON);

        // Verify cap-drop ALL
        int capDropIndex = command.indexOf("--cap-drop");
        assertTrue(capDropIndex != -1, "Command must contain --cap-drop");
        assertEquals("ALL", command.get(capDropIndex + 1));

        // Verify no-new-privileges:true
        int secOptIndex = command.indexOf("--security-opt");
        assertTrue(secOptIndex != -1, "Command must contain --security-opt");
        assertEquals("no-new-privileges:true", command.get(secOptIndex + 1));

        // Verify read-only root
        assertTrue(command.contains("--read-only"), "Command must contain --read-only for root filesystem");

        // Verify bounded tmpfs /tmp
        int tmpfsIndex = command.indexOf("--tmpfs");
        assertTrue(tmpfsIndex != -1, "Command must contain --tmpfs");
        assertEquals("/tmp:rw,nosuid,size=64m", command.get(tmpfsIndex + 1));

        // Verify network isolation
        int netIndex = command.indexOf("--network");
        assertTrue(netIndex != -1, "Command must contain --network");
        assertEquals("none", command.get(netIndex + 1));

        // Verify resource limits
        assertTrue(command.contains("--memory"));
        assertTrue(command.contains("--cpus"));
        assertTrue(command.contains("--pids-limit"));

        // Verify workspace mounting
        assertTrue(command.contains("-w"));
        assertTrue(command.contains("/workspace"));
    }

    @Test
    @DisplayName("Container options respect configurable security toggles")
    void testConfigurableSecurityToggles(@TempDir Path tempDir) {
        ExecutionWorkerProperties props = new ExecutionWorkerProperties();
        props.setReadOnlyRoot(false);
        props.setDropAllCapabilities(false);
        props.setNoNewPrivileges(false);
        props.setTmpfsMount(null);

        DockerSandboxService service = new DockerSandboxService(props);
        List<String> command = service.buildDockerRunCommand("cloudide-test-container", tempDir, ProgrammingLanguage.PYTHON);

        assertFalse(command.contains("--read-only"));
        assertFalse(command.contains("--cap-drop"));
        assertFalse(command.contains("--security-opt"));
        assertFalse(command.contains("--tmpfs"));
    }
}
