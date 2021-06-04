provider "aws" {
    profile = "default"
    region = "us-east-1"
}

terraform {
  backend "s3" {
    bucket         = "tf-remote-state-el"
    key            = "917346298954/tf.state"
    dynamodb_table = "tf-remote-state-el"
    region         = "us-east-1"
  }
}
