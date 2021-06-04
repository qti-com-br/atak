*** Settings ***
Documentation           A test for spreading a 8mb file across a network of 2 VIN-enabled instances

Resource                ../resources/keywords.robot
Suite Setup              Terraform Mars
Test Teardown           Shutdown All Nodes

*** Test Cases ***
Share 0.25mb
    @{ips}=                 Get Test Ips    ../resources/assets/ips.json
    
    SSH Connection          ${ips[0]}       _alias=receiver
    SSH Connection          ${ips[1]}       _alias=sender
    SSH Connection          ${ips[2]}       _alias=bootstrap

    Execute Command         touch ~/boot_cmds.txt
    Execute Command         echo > ~/boot_cmds.txt
    Start Command           VIN -t ~/boot_cmds.txt -b > /dev/null &     sudo=True

    Switch Connection       receiver
    Execute Command         sed -i "2s/0.0.0.0/${ips[2]}/" /etc/opt/VIN/defaults.cfg
    Execute Command         touch ~/recv_cmds.txt
    Execute Command         echo > ~/recv_cmds.txt
    Start Command           VIN -t ~/recv_cmds.txt > /dev/null &    sudo=True

    Sleep                   2s

    Switch Connection       sender
    Execute Command         sed -i "2s/0.0.0.0/${ips[2]}/" /etc/opt/VIN/defaults.cfg
    Put File                ../resources/assets/${test_file}
    Execute Command         touch ~/send_cmds.txt
    Execute Command         echo > ~/send_cmds.txt
    Start Command           VIN -t ~/send_cmds.txt > /dev/null &    sudo=True

    Sleep                   2s
    Execute Command         echo " share /home/ubuntu/${test_file} ${ips[0]} 9090" > ~/send_cmds.txt 
    Sleep                   2s

    Switch Connection       bootstrap
    Execute Command         echo "exit" >> ~/boot_cmds.txt
    Switch Connection       sender
    Execute Command         echo "exit" >> ~/send_cmds.txt
    Switch Connection       receiver
    Execute Command         echo "exit" >> ~/recv_cmds.txt



    Get File                /opt/VIN/outputs/rebuilt*    ../resources/assets/rebuilt.jpg

    Files Should Be Equal   ../resources/assets/rebuilt.jpg   ../resources/assets/${test_file}

    Close All Connections

