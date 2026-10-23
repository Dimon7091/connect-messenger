export interface User {
  id: string
  userName: string
  email: string
  createdAt: Date | string
  profile: {
    firstName?: string
    lastName?: string | null
    avatarUrl?: string | null
    avatarThumbUrl?: string | null
  }
  isDeleted: boolean
  isBanned: boolean
  blackListIds?: string[]
}

export interface UserStatus {
  userId: string
  status: string
  lastSeen?: string
}

export interface UserBlockResponser {
  blockedUserId: string
}

export interface UsersGettingByIdsRequest {
  usersIds: string[]
}
