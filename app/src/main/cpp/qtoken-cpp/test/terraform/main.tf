resource "aws_instance" "bootstrap" {
  count           = var.instance_count
  ami             = data.aws_ami.ubuntu2004.id
  instance_type   = var.instance_type

  key_name = aws_key_pair.generated_key.key_name
  security_groups = [ "p2p" ]

  tags            = {
    Name = "EL_test-${count.index+1}"
  }

  # iam_instance_profile = aws_iam_instance_profile.instance_profile.name

  user_data       = file("install-vin.sh")
}

resource "tls_private_key" "aws_key" {
  algorithm   = "RSA"
  rsa_bits    = 4096
}

resource "aws_key_pair" "generated_key" {
  key_name    = var.key_name
  public_key  = tls_private_key.aws_key.public_key_openssh
}

output "test_ips" {
  value       = aws_instance.bootstrap.*.public_ip
}

output "ssh_key" {
  value       = tls_private_key.aws_key.private_key_pem
}

output "pub_pem" {
  value       = tls_private_key.aws_key.public_key_pem
}

output "pub_key_openssh" {
  value       = tls_private_key.aws_key.public_key_openssh
}