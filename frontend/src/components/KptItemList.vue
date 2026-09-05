<template>
  <div class="card kpt-card" :style="{ borderLeftColor: tagColor }">
    <div class="kpt-header">
      <div class="kpt-tag" :style="{ background: tagColor }">{{ tagLabel }}</div>
      <span class="kpt-title">{{ title }}</span>
      <span class="kpt-count">{{ store.itemCount(type) }}/20</span>
      <button class="btn-guide" @click="$emit('guide')">?</button>
      <button class="btn-load" :disabled="!editable" @click="showPicker = true">불러오기</button>
    </div>

    <draggable
      v-model="savedItems"
      item-key="id"
      handle=".drag-handle"
      :disabled="!editable"
      animation="150"
      class="item-list"
    >
      <template #item="{ element, index }">
        <div class="item-row">
          <span class="drag-handle" v-if="editable">⠿</span>
          <span class="item-index">{{ prefix }}{{ index + 1 }}</span>
          <textarea
            :value="element.content"
            :disabled="!editable"
            rows="2"
            @input="onInput(element, $event.target.value)"
          />
          <button v-if="editable" class="btn-del" @click="onDelete(element)">✕</button>
        </div>
      </template>
    </draggable>

    <div v-if="editable && draftItem" class="item-row draft-row">
      <span class="drag-handle placeholder" />
      <span class="item-index muted">{{ prefix }}{{ savedItems.length + 1 }}</span>
      <textarea
        :value="draftItem.content"
        :placeholder="placeholder"
        rows="2"
        @input="onInput(draftItem, $event.target.value)"
      />
    </div>

    <slot name="extra" />

    <KptMemoPickerModal
      v-if="showPicker"
      :type="type"
      :remaining="20 - store.itemCount(type)"
      @close="showPicker = false"
      @confirm="onPickerConfirm"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import draggable from 'vuedraggable'
import { useRetrospectStore } from '@/stores/retrospect'
import KptMemoPickerModal from './KptMemoPickerModal.vue'

const props = defineProps({
  type: String,
  title: String,
  tagLabel: String,
  tagColor: String,
  placeholder: String,
  editable: Boolean
})
defineEmits(['guide'])

const store = useRetrospectStore()
const showPicker = ref(false)

const prefix = computed(() => ({ KEEP: 'K', PROBLEM: 'P', TRY: 'T' }[props.type]))

const savedItems = computed({
  get: () => store.items[props.type].filter(i => i.id != null),
  set: (newOrder) => {
    const draft = store.items[props.type].find(i => i.id == null)
    store.items[props.type] = draft ? [...newOrder, draft] : [...newOrder]
    store.reorderItems(props.type, newOrder)
  }
})

const draftItem = computed(() => store.items[props.type].find(i => i.id == null))

function onInput(item, value) {
  item.content = value
  store.scheduleItemSave(item, props.type)
}

function onDelete(item) {
  store.deleteItem(item, props.type)
}

function onPickerConfirm(memoIds) {
  store.addItemsFromMemo(props.type, memoIds)
  showPicker.value = false
}
</script>

<style scoped>
.card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.kpt-card { border-left: 4px solid transparent; }

.kpt-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.kpt-tag {
  width: 26px; height: 26px; border-radius: 7px;
  display: flex; align-items: center; justify-content: center;
  font-size: 0.8rem; font-weight: 800; color: #fff; flex-shrink: 0;
}
.kpt-title { font-size: 0.875rem; font-weight: 600; color: #374151; flex: 1; }
.kpt-count { font-size: 0.75rem; color: #94a3b8; font-weight: 600; }

.btn-guide {
  width: 22px; height: 22px; border-radius: 50%;
  background: #f1f5f9; border: none; color: #94a3b8;
  font-size: 0.75rem; font-weight: 700; cursor: pointer; flex-shrink: 0;
}
.btn-load {
  font-size: 0.75rem; font-weight: 600; color: #6366f1;
  background: #eef2ff; border: none; border-radius: 8px;
  padding: 5px 10px; cursor: pointer; white-space: nowrap;
}
.btn-load:disabled { opacity: 0.5; cursor: not-allowed; }

.item-list { display: flex; flex-direction: column; gap: 8px; }
.item-row {
  display: flex; align-items: flex-start; gap: 8px;
  padding: 4px 0;
}
.drag-handle {
  cursor: grab; color: #cbd5e1; font-size: 1rem; padding-top: 10px; user-select: none;
}
.drag-handle.placeholder { visibility: hidden; }
.item-index {
  flex-shrink: 0; width: 28px; padding-top: 10px;
  font-size: 0.75rem; font-weight: 700; color: #94a3b8;
}
.item-index.muted { color: #cbd5e1; }

textarea {
  flex: 1; border: 1.5px solid #e2e8f0; border-radius: 10px;
  padding: 10px 12px; font-size: 0.875rem; line-height: 1.6;
  resize: vertical; min-height: 44px; color: #374151;
  font-family: inherit; transition: border-color 0.15s;
}
textarea:focus { outline: none; border-color: #6366f1; }
textarea:disabled { background: #f8fafc; color: #64748b; }

.btn-del {
  flex-shrink: 0; width: 26px; height: 26px; margin-top: 6px;
  border-radius: 50%; background: #fef2f2; color: #ef4444;
  border: none; font-size: 0.7rem; cursor: pointer;
}

.draft-row { opacity: 0.85; }
</style>
