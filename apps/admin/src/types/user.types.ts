export interface Profile {
  firstName: string
  lastName: string
  avatarUrl?: string | null
  avatarThumbUrl?: string | null
}

export interface User {
  id: string
  email: string
  roles: string[]
  createdAt: string // Можно использовать Date, если строка парсится при получении
  profile: Profile
  userName: string
  status: string
  lastSeen: string
  isBanned: boolean
  isDeleted: boolean
}
