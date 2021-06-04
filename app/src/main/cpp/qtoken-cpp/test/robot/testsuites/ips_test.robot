*** Settings ***
Documentation           A test for spreading a 8mb file across a network of 2 VIN-enabled instances

Resource                ../resources/keywords.robot
Test Setup              Terraform Mars
Test Teardown           Shutdown All Nodes

*** Test Cases ***
Test
    No Operation

