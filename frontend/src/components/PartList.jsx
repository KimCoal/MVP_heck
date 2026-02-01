import { useEffect, useState } from 'react'
import { getCadFileById } from '../api/cadApi'

function PartList({ cadFileId, parts: initialParts, onPartSelect, selectedPartId }) {
  const [parts, setParts] = useState(initialParts || [])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    setParts(initialParts || [])
  }, [initialParts])

  useEffect(() => {
    // 주기적으로 부품 목록 갱신 (처리 중일 때)
    if (cadFileId && parts.length === 0) {
      const interval = setInterval(async () => {
        try {
          setLoading(true)
          const file = await getCadFileById(cadFileId)
          setParts(file.parts || [])
          setLoading(false)
        } catch (error) {
          console.error('부품 목록 로드 실패:', error)
          setLoading(false)
        }
      }, 2000) // 2초마다 체크

      return () => clearInterval(interval)
    }
  }, [cadFileId, parts.length])

  if (loading) {
    return (
      <div className="part-list">
        <h2>부품 목록</h2>
        <p>로딩 중...</p>
      </div>
    )
  }

  return (
    <div className="part-list">
      <h2>부품 목록 ({parts.length})</h2>
      {parts.length === 0 ? (
        <p>부품이 없습니다.</p>
      ) : (
        <ul>
          {parts.map(part => (
            <li
              key={part.id}
              className={`part-item ${selectedPartId === part.id ? 'active' : ''}`}
              onClick={() => onPartSelect(part)}
            >
              <div className="part-name">{part.name || `부품 ${part.id}`}</div>
              {part.note && (
                <div className="part-note-indicator">📝</div>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}

export default PartList
