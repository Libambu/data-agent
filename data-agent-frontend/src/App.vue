<script setup lang="ts">
import { Client, ClientFactory } from '@a2a-js/sdk/client'
import { onMounted, reactive, ref, shallowRef, nextTick } from 'vue'
import ToyHelloNode from '@/components/toy-hello-node.vue'
import { getAgentCard } from '@/utils/api-instance.ts'

interface ToyStep {
  id: string
  name: string
  content: string
  status: 'pending' | 'success'
}
const factory = new ClientFactory()
const client = shallowRef<Client | undefined>()
const currentStep = reactive<ToyStep>({ content: '', id: '', name: '', status: 'pending' })
const userInput = ref('')
const waitForPaint = async () => {
  await nextTick()
  await new Promise<void>((resolve) => requestAnimationFrame(() => resolve()))
}
const handleSend = async () => {
  if (!client.value) return
  currentStep.id = crypto.randomUUID()
  currentStep.name = ''
  currentStep.content = ''
  currentStep.status = 'pending'
  const stream = client.value.sendMessageStream({
    message: {
      messageId: crypto.randomUUID(),
      role: 'user',
      kind: 'message',
      parts: [{ kind: 'text', text: userInput.value }],
    },
  })
  for await (const event of stream) {
    console.log(event)
    if (event.kind === 'artifact-update') {
      const artifact = event.artifact
      if (artifact.name !== 'TOY_HELLO_NODE') continue
      const textPart = artifact.parts.find((p) => p.kind === 'text')
      if (textPart) {
        currentStep.name = artifact.name
        currentStep.content += textPart.text
        currentStep.status = 'pending'
        await waitForPaint()
      }
    }
    if (event.kind === 'status-update' && event.status.state === 'completed') {
      currentStep.status = 'success'
    }
  }
}
onMounted(async () => {
  client.value = await factory.createFromAgentCard(await getAgentCard())
})
</script>

<template>
  <div>
    <toy-hello-node
      v-if="currentStep.name === 'TOY_HELLO_NODE'"
      :content="currentStep.content"
      :status="currentStep.status"
    />
    <el-input placeholder="请输入内容" v-model="userInput" @keydown.enter="handleSend"></el-input>
  </div>
</template>

<style scoped></style>
