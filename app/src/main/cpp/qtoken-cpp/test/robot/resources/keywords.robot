*** Settings ***
Resource                vars.robot
Library                 SSHLibrary
Library                 terraform_mesh.py

*** Keywords ***
SSH Connection
    [Arguments]         ${_ip}      ${_alias}
    Open Connection         ${_ip}      alias=${_alias}
    Login With Public Key   ${username}     ../resources/assets/${ssh_key}

Terraform Mars
    @{ips}=                 Get Test Ips        ../resources/assets/ips.json
    FOR                 ${node}     IN      @{ips}
        AWS Ready Check     ${node}     ../resources/assets/${ssh_key}       ${ready_file}
        SSH Connection      ${node}     ${node}
        Put File            ../resources/assets/VIN_${version}.deb
        ${rc}=      Execute Command     apt update -y                   sudo=True   return_stdout=False  return_rc=True
        Should Be Equal As Integers     ${rc}   0
        ${rc}=      Execute Command     apt install -y /home/ubuntu/VIN_${version}.deb      sudo=True   return_stdout=False  return_rc=True
        Should Be Equal As Integers     ${rc}   0
        ${rc}=      Execute Command     apt --fix-broken install -y     sudo=True   return_stdout=False  return_rc=True
        Should Be Equal As Integers     ${rc}   0

        Execute Command     rm /opt/VIN/outputs/*
        Close Connection
    END

Shutdown All Nodes
    No Operation