import json
import paramiko
import filecmp
from robot.api.deco import keyword

__version__ = '0.1'

@keyword('Get Test Ips')
def get_test_ips(ip_file):
    with open(ip_file) as f:
        data = json.load(f)
    return data

@keyword('Files Should Be Equal')
def cmp_files(fd1, fd2):
    assert True == filecmp.cmp(fd1, fd2), "Files not equal"

@keyword('AWS Ready Check')
def ready_check(server_ip, ssh_key, ready_file):
    count = 0
    maxTries = 100
    while(True):
        try:
            client = paramiko.SSHClient()
            key = paramiko.RSAKey.from_private_key_file(ssh_key)
            client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
            client.connect(server_ip, username="ubuntu", pkey=key)
            break
        except:
            count += 1
            if(count == maxTries):
                raise Exception("NoValidConnections")
        


    exit_code=2
    while(not(exit_code == 0)):
        stdin, stdout, stderr = client.exec_command('ls /tmp/' + ready_file)
        exit_code = stdout.channel.recv_exit_status()
        

