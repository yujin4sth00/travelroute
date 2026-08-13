import { DndContext, closestCenter } from '@dnd-kit/core'
import { SortableContext, verticalListSortingStrategy, arrayMove } from '@dnd-kit/sortable'
import SortablePlaceItem from './SortablePlaceItem'

export default function DayPlaceList({ places, onReorder, onRemove }) {
  const handleDragEnd = (event) => {
    const { active, over } = event
    if (!over || active.id === over.id) return

    const oldIndex = places.findIndex((p) => p.id === active.id)
    const newIndex = places.findIndex((p) => p.id === over.id)
    const reordered = arrayMove(places, oldIndex, newIndex)
    onReorder(reordered.map((p) => p.id))
  }

  if (places.length === 0) {
    return <p className="muted">이 날짜에 배치된 장소가 없습니다.</p>
  }

  return (
    <DndContext collisionDetection={closestCenter} onDragEnd={handleDragEnd}>
      <SortableContext items={places.map((p) => p.id)} strategy={verticalListSortingStrategy}>
        <ul className="day-place-list">
          {places.map((place) => (
            <SortablePlaceItem key={place.id} place={place} onRemove={onRemove} />
          ))}
        </ul>
      </SortableContext>
    </DndContext>
  )
}
