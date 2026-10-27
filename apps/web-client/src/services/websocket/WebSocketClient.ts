// services/websocket/StompWebSocketClient.ts
import { Client } from '@stomp/stompjs'
import type { IFrame, IMessage, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { WSEventType } from '@/types/index'

type EventHandler<T = unknown> = (payload: T) => void
type UniversalHandler = (type: WSEventType, payload: unknown) => void

interface QueuedMessage {
  destination: string
  body: string
}

export class StompWebSocketClient {
  private client: Client | null = null
  private url: string
  private token: string | null = null
  private messageHandlers: Map<WSEventType | '*', Set<EventHandler<unknown>>> = new Map()
  private subscriptions: Map<string, StompSubscription> = new Map()
  private isManualDisconnect = false
  private reconnectAttempts = 0
  private maxReconnectAttempts = 5
  private reconnectDelay = 1000
  private messageQueue: QueuedMessage[] = []

  constructor(url: string) {
    this.url = url
  }

  // Геттер для проверки реального статуса соединения
  get isConnected(): boolean {
    return !!this.client && this.client.connected
  }

  connect(token: string): void {
    if (this.client?.active) {
      console.log('STOMP: Already active or connecting')
      return
    }

    this.token = token
    this.isManualDisconnect = false

    this.client = new Client({
      // Новая фабрика SockJS генерирует свежий инстанс при каждом переподключении
      webSocketFactory: () => new SockJS(`${this.url}?token=${this.token}`),

      connectHeaders: {
        'Auth-Token': token,
      },

      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      debug: (str) => {
        if (import.meta.env.DEV) {
          console.debug('STOMP:', str)
        }
      },

      onConnect: (frame: IFrame) => {
        console.log('✅ STOMP: Connected', frame)
        this.reconnectAttempts = 0
        this.dispatch('connection', { status: 'connected' })

        // Подписываемся на личную очередь статусов
        this.subscribeToStatusQueue()

        // Восстанавливаем сохраненные подписки на чаты
        this.restoreSubscriptions()
        // Отправляем сообщения, накопившиеся во время простоя
        this.flushQueue()
      },

      onStompError: (frame: IFrame) => {
        console.error('❌ STOMP: Error', frame)
        this.dispatch('error', {
          message: frame.headers['message'] ?? 'Unknown error',
          body: frame.body,
        })
      },

      onDisconnect: (frame: IFrame) => {
        console.log('🔌 STOMP: Disconnected', frame)
        if (!this.isManualDisconnect) {
          this.dispatch('connection', { status: 'disconnected' })
        }
      },

      onWebSocketError: (event: Event) => {
        console.error('❌ STOMP: WebSocket error', event)
        this.dispatch('error', { message: 'WebSocket connection error' })
      },

      onWebSocketClose: (event: CloseEvent) => {
        console.log('🔌 WebSocket closed:', event.code, event.reason)

        if (!this.isManualDisconnect) {
          this.dispatch('connection', { status: 'disconnected' })

          // Очищаем дескрипторы, чтобы restoreSubscriptions мог пересоздать подписки заново
          this.subscriptions.clear()

          // Запускаем механизм реконнекта
          this.handleReconnection()
        }
      },
    })

    this.client.activate()
  }

  // Подписка на очередь статусов (личная)
  private subscribeToStatusQueue(): void {
    const destination = '/user/queue/status'
    if (this.subscriptions.has(destination)) return

    const subscription = this.client!.subscribe(destination, (message: IMessage) => {
      try {
        const event = JSON.parse(message.body)
        if (event.type && event.payload) {
          this.dispatch(event.type as WSEventType, event.payload)
        } else {
          console.warn('Received malformed status event', event)
        }
      } catch (e) {
        console.error('Failed to parse status message', e)
      }
    })
    this.subscriptions.set(destination, subscription)
    console.log(`✅ STOMP: Subscribed to ${destination}`)
  }

  // Восстановление подписок
  private restoreSubscriptions(): void {
    const oldDestinations = Array.from(this.subscriptions.keys())
    this.subscriptions.clear()

    for (const destination of oldDestinations) {
      if (destination === '/user/queue/status') continue
      this.subscribeToDestination(destination)
    }
  }

  private subscribeToDestination(destination: string): void {
    if (!this.client || !this.client.connected) return
    if (this.subscriptions.has(destination)) return

    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const parsed = JSON.parse(message.body)
        if (typeof parsed.type === 'string' && parsed.payload !== undefined) {
          this.dispatch(parsed.type as WSEventType, parsed.payload)
        } else {
          console.warn('Message without type/payload', parsed)
        }
      } catch (e) {
        console.error('Failed to parse message', e)
      }
    })
    this.subscriptions.set(destination, subscription)
    console.log(`✅ STOMP: Subscribed to ${destination}`)
  }

  // Отправка сообщения в едином формате WSEvent
  send<T = unknown>(type: WSEventType, payload: T, destination?: string): void {
    const dest = destination || `/app/${type}`
    const body = JSON.stringify({ type, payload })

    if (this.isConnected) {
      this.client!.publish({
        destination: dest,
        body: body,
        headers: { 'content-type': 'application/json' },
      })
      console.log(`📤 STOMP: Sent ${type} to ${dest}`)
    } else {
      console.log(`⏳ STOMP: Connection inactive. Queueing message ${type}`)
      this.messageQueue.push({ destination: dest, body })
    }
  }

  subscribeToUserStatus(targetUserId: string | number): void {
    this.send('user_status_subscribe', { targetUserId })
  }

  unsubscribeFromUserStatus(targetUserId: string | number): void {
    this.send('user_status_unsubscribe', { targetUserId })
  }

  // Подписка на чат
  subscribeToChat(chatKey: string): void {
    const destination = `/topic/chats/${chatKey}`
    this.subscribeToDestination(destination)
  }

  unsubscribeFromChat(chatKey: string): void {
    const destination = `/topic/chats/${chatKey}`
    const subscription = this.subscriptions.get(destination)
    if (subscription) {
      subscription.unsubscribe()
      this.subscriptions.delete(destination)
      console.log(`❌ STOMP: Unsubscribed from ${destination}`)
    }
  }

  subscribeToUserQueue(): void {
    const destination = `/user/queue/private`
    this.subscribeToDestination(destination)
  }

  sendToUser<T = unknown>(userId: string, type: WSEventType, payload: T): void {
    const destination = `/user/${userId}/queue/private`
    const body = JSON.stringify({ type, payload })

    if (this.isConnected) {
      this.client!.publish({ destination, body })
    } else {
      console.log(`⏳ STOMP: Queueing private message to user ${userId}`)
      this.messageQueue.push({ destination, body })
    }
  }

  private flushQueue(): void {
    while (this.messageQueue.length > 0 && this.isConnected) {
      const queuedMessage = this.messageQueue.shift()
      if (queuedMessage) {
        this.client!.publish({
          destination: queuedMessage.destination,
          body: queuedMessage.body,
          headers: { 'content-type': 'application/json' },
        })
        console.log(`📤 STOMP: Flushed message from queue to ${queuedMessage.destination}`)
      }
    }
  }

  private handleReconnection(): void {
    if (this.isManualDisconnect) return

    if (this.client) {
      this.client.deactivate()
      this.client = null
    }

    if (this.reconnectAttempts < this.maxReconnectAttempts && this.token) {
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts)
      this.reconnectAttempts++

      console.log(
        `🔄 STOMP: Scheduling reconnection attempt ${this.reconnectAttempts} in ${delay}ms`,
      )

      setTimeout(() => {
        if (this.isManualDisconnect) return
        this.connect(this.token!)
      }, delay)
    } else {
      console.error('❌ STOMP: Max reconnection attempts reached')
      this.dispatch('connection', { status: 'failed' })
    }
  }

  disconnect(): void {
    this.isManualDisconnect = true
    if (this.client) {
      this.client.deactivate()
      this.client = null
    }
    this.token = null
    this.subscriptions.clear()
    this.messageQueue = []
    this.messageHandlers.clear()
    console.log('🔌 STOMP: Disconnected and cleaned up')
  }

  on<T = unknown>(type: WSEventType | '*', handler: EventHandler<T>): void {
    if (!this.messageHandlers.has(type)) {
      this.messageHandlers.set(type, new Set())
    }
    this.messageHandlers.get(type)!.add(handler as EventHandler<unknown>)
  }

  off(type: WSEventType | '*', handler?: EventHandler<unknown>): void {
    if (!handler) {
      this.messageHandlers.delete(type)
      return
    }
    const handlers = this.messageHandlers.get(type)
    if (handlers) {
      handlers.delete(handler as EventHandler<unknown>)
      if (handlers.size === 0) {
        this.messageHandlers.delete(type)
      }
    }
  }

  private dispatch(type: WSEventType | 'connection' | 'error', payload: unknown): void {
    const handlers = this.messageHandlers.get(type as WSEventType)
    if (handlers) {
      handlers.forEach((handler) => {
        try {
          handler(payload)
        } catch (e) {
          console.error(`Error in event handler for ${type}:`, e)
        }
      })
    }

    const globalHandlers = this.messageHandlers.get('*')
    if (globalHandlers) {
      globalHandlers.forEach((handler) => {
        try {
          handler({ type, payload })
        } catch (e) {
          console.error(`Error in global event handler:`, e)
        }
      })
    }
  }
}

const WS_URL = import.meta.env.VITE_WS_URL || '/chat-ws'
export const wsClient = new StompWebSocketClient(WS_URL)
