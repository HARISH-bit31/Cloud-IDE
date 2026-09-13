# Cloud IDE — Phase 9.2 Comprehensive Kubernetes Integration Test Suite

$baseUrl = "http://localhost:30080"
$results = @{}

Write-Host "=========================================================="
Write-Host "STARTING PHASE 9.2 KUBERNETES EXECUTION WORKER TEST SUITE"
Write-Host "=========================================================="

# 1. Login
Write-Host "`n--- TEST 1: LOGIN ---"
$loginBody = @{email="alex.developer@cloud-ide.io"; password="password123"} | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method Post -ContentType "application/json" -Body $loginBody
$token = $loginRes.token
$headers = @{"Authorization"="Bearer $token"}
Write-Host "Token received:" ($token -ne $null) "User:" $loginRes.user.email
$results["Test 01: Login"] = ($token -ne $null)

# 2. Dashboard / Project Loading
Write-Host "`n--- TEST 2: DASHBOARD / PROJECT LOADING ---"
$projects = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Get -Headers $headers
Write-Host "Loaded projects count:" $projects.Count
$results["Test 02: Project Loading"] = ($projects.Count -ge 1)

# 3. Create Project
Write-Host "`n--- TEST 3: CREATE PROJECT ---"
$rand = Get-Random
$projName = "K8s Phase 9.2 Test $rand"
$projBody = @{name=$projName; description="Phase 9.2 Verification"; language="JAVA"} | ConvertTo-Json
$createdProj = Invoke-RestMethod -Uri "$baseUrl/api/projects" -Method Post -ContentType "application/json" -Headers $headers -Body $projBody
Write-Host "Created Project ID:" $createdProj.id "Name:" $createdProj.name
$results["Test 03: Create Project"] = ($createdProj.id -gt 0)

# 4. Create / Update File
Write-Host "`n--- TEST 4: CREATE / UPDATE FILE ---"
$fileBody = @{filename="Main.java"; content="public class Main { public static void main(String[] args) { System.out.println(""Initial""); } }"} | ConvertTo-Json
$createdFile = Invoke-RestMethod -Uri "$baseUrl/api/projects/$($createdProj.id)/files" -Method Post -ContentType "application/json" -Headers $headers -Body $fileBody
$updateBody = @{filename="Main.java"; content="public class Main { public static void main(String[] args) { System.out.println(""Updated""); } }"} | ConvertTo-Json
$updatedFile = Invoke-RestMethod -Uri "$baseUrl/api/projects/$($createdProj.id)/files/$($createdFile.id)" -Method Put -ContentType "application/json" -Headers $headers -Body $updateBody
Write-Host "File ID:" $updatedFile.id "Content contains Updated:" ($updatedFile.content -match "Updated")
$results["Test 04: Create/Update File"] = ($updatedFile.content -match "Updated")

# Helper function to run code and poll
function Run-CodeAndPoll($lang, $code, $stdin="", $timeout=10) {
    $req = @{language=$lang; code=$code; stdin=$stdin; timeoutSeconds=$timeout} | ConvertTo-Json
    $exec = Invoke-RestMethod -Uri "$baseUrl/api/execute" -Method Post -ContentType "application/json" -Headers $headers -Body $req
    $id = $exec.executionId
    
    for ($i = 0; $i -lt 25; $i++) {
        Start-Sleep -Milliseconds 600
        $status = Invoke-RestMethod -Uri "$baseUrl/api/execute/$id" -Headers $headers
        if ($status.status -ne "RUNNING" -and $status.status -ne "QUEUED" -and $status.status -ne "WAITING_FOR_INPUT") {
            return $status
        }
    }
    return $status
}

# 5. Java Hello World
Write-Host "`n--- TEST 5: JAVA HELLO WORLD ---"
$javaCode = 'public class Main { public static void main(String[] args) { System.out.println("Hello from K8s Java"); } }'
$res5 = Run-CodeAndPoll "JAVA" $javaCode
Write-Host "Status:" $res5.status "Stdout:" $res5.stdout.Trim() "ExitCode:" $res5.exitCode
$results["Test 05: Java Hello World"] = ($res5.status -eq "SUCCESS" -and $res5.stdout -match "Hello from K8s Java")

# 6. Python Program
Write-Host "`n--- TEST 6: PYTHON PROGRAM ---"
$pyCode = 'print("Hello from K8s Python")'
$res6 = Run-CodeAndPoll "PYTHON" $pyCode
Write-Host "Status:" $res6.status "Stdout:" $res6.stdout.Trim() "ExitCode:" $res6.exitCode
$results["Test 06: Python Program"] = ($res6.status -eq "SUCCESS" -and $res6.stdout -match "Hello from K8s Python")

# 7. C Program
Write-Host "`n--- TEST 7: C PROGRAM ---"
$cCode = @"
#include <stdio.h>
int main() {
    printf("Hello from K8s C\n");
    return 0;
}
"@
$res7 = Run-CodeAndPoll "C" $cCode
Write-Host "Status:" $res7.status "Stdout:" $res7.stdout.Trim() "ExitCode:" $res7.exitCode
$results["Test 07: C Program"] = ($res7.status -eq "SUCCESS" -and $res7.stdout -match "Hello from K8s C")

# 8. C++ Program
Write-Host "`n--- TEST 8: C++ PROGRAM ---"
$cppCode = @"
#include <iostream>
int main() {
    std::cout << "Hello from K8s CPP" << std::endl;
    return 0;
}
"@
$res8 = Run-CodeAndPoll "CPP" $cppCode
Write-Host "Status:" $res8.status "Stdout:" $res8.stdout.Trim() "ExitCode:" $res8.exitCode
$results["Test 08: C++ Program"] = ($res8.status -eq "SUCCESS" -and $res8.stdout -match "Hello from K8s CPP")

# 9. Java Interactive Scanner Input
Write-Host "`n--- TEST 9: JAVA INTERACTIVE SCANNER INPUT ---"
$scannerCode = 'import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); System.out.println("Enter number:"); int n = sc.nextInt(); System.out.println("Square = " + (n * n)); } }'
$startReq = @{language="JAVA"; code=$scannerCode} | ConvertTo-Json
$exec9 = Invoke-RestMethod -Uri "$baseUrl/api/execute" -Method Post -ContentType "application/json" -Headers $headers -Body $startReq
$id9 = $exec9.executionId
Write-Host "Started execution $id9. Waiting 1.2s then sending input '25'..."
Start-Sleep -Milliseconds 1200
$inputReq = @{input="25`n"} | ConvertTo-Json
$inputRes = Invoke-RestMethod -Uri "$baseUrl/api/execute/$id9/input" -Method Post -ContentType "application/json" -Headers $headers -Body $inputReq
Write-Host "Input response:" ($inputRes | ConvertTo-Json -Compress)

for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep -Milliseconds 600
    $res9 = Invoke-RestMethod -Uri "$baseUrl/api/execute/$id9" -Headers $headers
    if ($res9.status -ne "RUNNING" -and $res9.status -ne "WAITING_FOR_INPUT") {
        break
    }
}
Write-Host "Status:" $res9.status "Stdout:" $res9.stdout.Trim()
$results["Test 09: Java Scanner Input"] = ($res9.status -eq "SUCCESS" -and $res9.stdout -match "Square = 625")

# 10. Compilation Error
Write-Host "`n--- TEST 10: COMPILATION ERROR ---"
$compErrCode = 'public class Main { public static void main(String[] args) { System.out.println("No semicolon") } }'
$res10 = Run-CodeAndPoll "JAVA" $compErrCode
Write-Host "Status:" $res10.status "Stderr contains expected error:" ($res10.stderr -match "error: ';' expected")
$results["Test 10: Compilation Error"] = ($res10.status -eq "COMPILATION_ERROR" -and $res10.stderr -match "error")

# 11. Runtime Error
Write-Host "`n--- TEST 11: RUNTIME ERROR ---"
$runtimeErrCode = 'public class Main { public static void main(String[] args) { int x = 10 / 0; } }'
$res11 = Run-CodeAndPoll "JAVA" $runtimeErrCode
Write-Host "Status:" $res11.status "Stderr contains ArithmeticException:" ($res11.stderr -match "ArithmeticException")
$results["Test 11: Runtime Error"] = ($res11.status -eq "RUNTIME_ERROR" -and $res11.stderr -match "ArithmeticException")

# 12. Timeout
Write-Host "`n--- TEST 12: TIMEOUT LOOP ---"
$loopCode = 'public class Main { public static void main(String[] args) { while(true){} } }'
$res12 = Run-CodeAndPoll "JAVA" $loopCode "" 3
Write-Host "Status:" $res12.status "ExitCode:" $res12.exitCode
$results["Test 12: Timeout"] = ($res12.status -eq "TIMEOUT" -and $res12.exitCode -eq 124)

# 13. Stop
Write-Host "`n--- TEST 13: STOP ---"
$stopCode = 'public class Main { public static void main(String[] args) throws Exception { java.lang.Thread.sleep(8000); } }'
$startReq13 = @{language="JAVA"; code=$stopCode} | ConvertTo-Json
$exec13 = Invoke-RestMethod -Uri "$baseUrl/api/execute" -Method Post -ContentType "application/json" -Headers $headers -Body $startReq13
$id13 = $exec13.executionId
Write-Host "Started long running execution $id13. Sending stop..."
Start-Sleep -Milliseconds 800
$stopRes = Invoke-RestMethod -Uri "$baseUrl/api/execute/$id13/stop" -Method Post -Headers $headers
Write-Host "Stop status:" $stopRes.status "ExitCode:" $stopRes.exitCode
$results["Test 13: Stop"] = ($stopRes.status -eq "STOPPED" -and $stopRes.exitCode -eq 130)

# 14. Concurrent Executions
Write-Host "`n--- TEST 14: CONCURRENT EXECUTIONS ---"
$c1Code = 'import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); int a = sc.nextInt(); System.out.println("ExecA = " + (a * 5)); } }'
$c2Code = 'import java.util.Scanner; public class Main { public static void main(String[] args) { Scanner sc = new Scanner(System.in); int b = sc.nextInt(); System.out.println("ExecB = " + (b * 30)); } }'

$e1 = Invoke-RestMethod -Uri "$baseUrl/api/execute" -Method Post -ContentType "application/json" -Headers $headers -Body (@{language="JAVA"; code=$c1Code} | ConvertTo-Json)
$e2 = Invoke-RestMethod -Uri "$baseUrl/api/execute" -Method Post -ContentType "application/json" -Headers $headers -Body (@{language="JAVA"; code=$c2Code} | ConvertTo-Json)

Start-Sleep -Milliseconds 800
Invoke-RestMethod -Uri "$baseUrl/api/execute/$($e1.executionId)/input" -Method Post -ContentType "application/json" -Headers $headers -Body (@{input="10`n"} | ConvertTo-Json) | Out-Null
Invoke-RestMethod -Uri "$baseUrl/api/execute/$($e2.executionId)/input" -Method Post -ContentType "application/json" -Headers $headers -Body (@{input="10`n"} | ConvertTo-Json) | Out-Null

for ($i = 0; $i -lt 15; $i++) {
    Start-Sleep -Milliseconds 600
    $r14A = Invoke-RestMethod -Uri "$baseUrl/api/execute/$($e1.executionId)" -Headers $headers
    $r14B = Invoke-RestMethod -Uri "$baseUrl/api/execute/$($e2.executionId)" -Headers $headers
    if ($r14A.status -eq "SUCCESS" -and $r14B.status -eq "SUCCESS") {
        break
    }
}

Write-Host "Exec A:" $r14A.stdout.Trim() "Status:" $r14A.status
Write-Host "Exec B:" $r14B.stdout.Trim() "Status:" $r14B.status
$results["Test 14: Concurrent Executions"] = ($r14A.stdout -match "ExecA = 50" -and $r14B.stdout -match "ExecB = 300")

# 15. Cleanup / Orphan Containers
Write-Host "`n--- TEST 15: CLEANUP / ORPHAN CONTAINERS ---"
Start-Sleep -Milliseconds 1500
$orphans = (docker ps -a --filter "name=cloudide-sandbox" --format "{{.Names}}")
Write-Host "Found sandbox containers count:" ($orphans.Length)
$results["Test 15: Container Cleanup"] = ($orphans.Length -eq 0 -or $orphans -eq "")

Write-Host "`n=========================================================="
Write-Host "SUMMARY OF PHASE 9.2 TEST RESULTS"
Write-Host "=========================================================="
foreach ($k in ($results.Keys | Sort-Object)) {
    Write-Host "$k : $($results[$k])"
}
