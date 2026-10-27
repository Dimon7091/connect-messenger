import type { User } from '@messenger/shared-schema'

export interface ApiResponse<T = unknown> {
  success: boolean
  data?: T
  error?: string
  message?: string
  status: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  user: User
  token: string
}

export interface RegisterRequest {
  userName: string
  password: string
  firstName?: string
  lastName?: string
  invitationToken: string
}

export interface UpdateUserNameRequest {
  username: string
}

export interface UpdateUserProfileRequest {
  firstName?: string
  lastName?: string | null
}

export interface FileInitRequest {
  fileName: string
  mimeType: string
  fileSize: string
}

export interface FileInitResponse {
  uploadUrl: string
  fileKey: string
  mimeType: string
  fileName: string
  size: string
}
