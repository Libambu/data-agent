import { request } from '@/utils/request'

export const getAgentCard = () => {
  return request({ url: '/.well-known/agent-card.json', method: 'get' })
}
