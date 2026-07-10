variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "Project name used as a prefix for all resource names"
  type        = string
  default     = "bubli"
}
