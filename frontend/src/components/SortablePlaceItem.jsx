import { useSortable } from '@dnd-kit/sortable'
import { CSS } from '@dnd-kit/utilities'

export default function SortablePlaceItem({ place, onRemove }) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: place.id,
  })

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.6 : 1,
  }

  return (
    <li ref={setNodeRef} style={style} className={`day-place-item ${place.locked ? 'locked' : ''}`}>
      <span className="drag-handle" {...attributes} {...listeners}>
        ⠿
      </span>
      <span className="order-badge">{place.visitOrder}</span>
      <span className="place-name">{place.placeName}</span>
      {place.locked && <span className="badge">고정됨</span>}
      <button className="danger" onClick={() => onRemove(place.id)}>
        삭제
      </button>
    </li>
  )
}
