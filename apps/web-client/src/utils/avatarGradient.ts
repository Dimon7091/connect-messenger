// utils/avatarGradient.ts

const GRADIENT_PALETTE = [
  'from-red-500 to-red-700',
  'from-blue-500 to-blue-700',
  'from-green-500 to-green-700',
  'from-yellow-500 to-yellow-700',
  'from-purple-500 to-purple-700',
  'from-pink-500 to-pink-700',
  'from-indigo-500 to-indigo-700',
  'from-teal-500 to-teal-700',
  'from-orange-500 to-orange-700',
  'from-cyan-500 to-cyan-700',
  'from-rose-500 to-rose-700',
  'from-violet-500 to-violet-700',
  'from-fuchsia-500 to-fuchsia-700',
  'from-emerald-500 to-emerald-700',
  'from-sky-500 to-sky-700',
  'from-amber-500 to-amber-700',
  'from-lime-500 to-lime-700',
  'from-blue-400 to-blue-600',
  'from-purple-400 to-purple-600',
  'from-green-400 to-green-600',
  'from-pink-400 to-pink-600',
  'from-red-400 to-red-600',
  'from-orange-400 to-orange-600',
  'from-cyan-400 to-cyan-600',
  'from-indigo-400 to-indigo-600',
]

export function getAvatarGradient(userId: string | number): string {
  const str = String(userId)
  let hash = 0
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i)
    hash |= 0
  }
  const index = Math.abs(hash) % GRADIENT_PALETTE.length
  return `bg-gradient-to-br ${GRADIENT_PALETTE[index]}`
}

export function getInitialsFromName(name: string): string {
  if (!name) return '?'

  const parts = name.trim().split(/\s+/).filter(Boolean)

  if (parts.length >= 2) {
    const first = parts[0]?.[0]
    const last = parts[parts.length - 1]?.[0]
    if (first && last) {
      return (first + last).toUpperCase()
    }
  }

  return parts[0]?.[0]?.toUpperCase() || '?'
}
