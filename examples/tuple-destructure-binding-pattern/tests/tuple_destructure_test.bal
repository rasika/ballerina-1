import ballerina/test;
import ballerina/io;

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
        outstr = outstr + io:sprintf("%s", str);
    }
    outputs[counter] = outstr;
    counter += 1;
}

@test:Config {}
function testFunc() {
    // Invoking the main function
    main();

    string out1 = "Tuple variable : Ballerina 453 false";
    string out2 = "Tuple variable : Ballerina 453 false";
    string out3 = "Tuple variable : Ballerina 657 true 76.8";
    string out4 = "Tuple variable : Ballerina 3 true 34 5.6 45";
    string out5 = "Tuple variable : Ballerina 453 false";
    string out6 = "Tuple variable : Ballerina 3 true 34 5.6 45";

    test:assertEquals(outputs[0], out1);
    test:assertEquals(outputs[1], out2);
    test:assertEquals(outputs[2], out3);
    test:assertEquals(outputs[3], out4);
    test:assertEquals(outputs[4], out5);
    test:assertEquals(outputs[5], out6);
}
