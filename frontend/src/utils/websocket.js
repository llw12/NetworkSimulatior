import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export class SimulationWebSocket {
  constructor() {
    this.client = null
    this.subscription = null
  }

  connect(projectId, onMessage, onError) {
    const socket = new SockJS('http://localhost:8080/ws/sim/log')
    
    this.client = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        console.log('WebSocket connected')
        this.subscription = this.client.subscribe(
          `/topic/sim/log/${projectId}`,
          (message) => {
            if (onMessage) {
              onMessage(message.body)
            }
          }
        )
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        if (onError) {
          onError(frame)
        }
      }
    })

    this.client.activate()
  }

  disconnect() {
    if (this.subscription) {
      this.subscription.unsubscribe()
    }
    if (this.client) {
      this.client.deactivate()
    }
  }
}
