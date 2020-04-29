import ballerina/test;

any[] outputs = [];
int counter = 0;

// This is the mock function which will replace the real function
@test:Mock {
    moduleName: "ballerina/io",
    functionName: "println"
}
public function mockPrint(any... s) {
    string outstr = "";
        foreach var str in s {
            outstr = outstr + str.toString();
        }
        outputs[counter] = outstr;
        counter += 1;
}

@test:Config {}
function testFunc() {
    // Invoking the main function
    main();
    test:assertEquals(outputs[0], "Name: Peter");
    test:assertEquals(outputs[1], "Age: 28");
    test:assertEquals(outputs[2], "Other Details: country=Sri Lanka occupation=Software Engineer");
    test:assertEquals(outputs[3], "Name: Peter");
    test:assertEquals(outputs[4], "Age: 28");
    test:assertEquals(outputs[5], "Name: Peter");
    test:assertEquals(outputs[6], "Age: 28");
    test:assertEquals(outputs[7], "Other Details: country=Sri Lanka occupation=Software Engineer");
    test:assertEquals(outputs[8], "Country Name: Sri Lanka");
    test:assertEquals(outputs[9], "Capital Name: Colombo");
}
