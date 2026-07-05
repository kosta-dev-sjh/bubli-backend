variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used as a prefix for resource names"
  type        = string
  default     = "bubli"
}

variable "s3_bucket_name" {
  description = "S3 bucket name for file storage. It must be globally unique across all AWS accounts."
  type        = string
}
