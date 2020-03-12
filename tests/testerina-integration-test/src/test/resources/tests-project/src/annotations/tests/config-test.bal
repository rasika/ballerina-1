import ballerina/test;

// This tests the functionality of test configs
// using int operations

// The following variables are expected to be changed when init reset and cleanup functions
// are called

int i = 0;
int j = 0;
int k = 0;

// Resets the value of i back to 0
function reset () {
    i=0;
}

// Changes the value of i from 0 to 1
function init () {
    i = 1;
}

// Sets back the up the value of i back to 0
function cleanup () {
    i = 0;
}

@test:Config{
    before: "init"
}
function testBefore () {
    test:assertTrue(i == 1, msg = "Expected i to be 1, but i = "+i.toString());
    reset();
}

@test:Config{
    before: "init", 
    after: "cleanup"
}
function test1 () {
    test:assertTrue(i == 1, msg = "Expected i to be 1, but i = "+i.toString());
}

@test:Config{
    dependsOn: ["test1"]
}
function testAfter () {
    test:assertTrue(i == 0, msg = "Expected i to be 0, but i = "+i.toString());
    reset();
}

@test:Config{
    after: "cleanup"
}
function test2 () {
    reset();
    test:assertTrue(i == 0, msg = "Expected i to be 0, but i = "+i.toString());
}

@test:Config{
    dependsOn: ["test2"]
}
function testAfterAlone () {
    test:assertTrue(i == 0, msg = "Expected i to be 0, but i = "+i.toString());
    reset();
}

@test:Config {}
function test3 () {
    j = j + 1;
}

@test:Config {}
function test4 () {
    j = j + 2;
}

@test:Config{
    dependsOn: ["test3", "test4", "test5"]
}
function testDependsOn1 () {
    test:assertTrue(j == 3, msg = "Expected j to be 3, but j = " +j.toString());
    test:assertTrue(k == 2, msg = "Expected k to be 2, but k = " +k.toString());
}

@test:Config {}
function test5 () {
    k = k + 2;
}
