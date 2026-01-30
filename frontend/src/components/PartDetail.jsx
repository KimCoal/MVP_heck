import { useState, useEffect } from 'react'
import { getPartById, savePartNote, deletePartNote } from '../services/api'

function PartDetail({ part: initialPart, onUpdate }) {
  const [part, setPart] = useState(initialPart)
  const [note, setNote] = useState(part?.note || '')
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (initialPart) {
      setPart(initialPart)
      setNote(initialPart.note || '')
    }
  }, [initialPart])

  const handleSaveNote = async () => {
    if (!part) return
    
    setSaving(true)
    try {
      await savePartNote(part.id, note)
      setEditing(false)
      if (onUpdate) {
        onUpdate()
      }
      // 부품 정보 갱신
      const updatedPart = await getPartById(part.id)
      setPart(updatedPart)
      setNote(updatedPart.note || '')
    } catch (error) {
      console.error('메모 저장 실패:', error)
      alert('메모 저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  const handleDeleteNote = async () => {
    if (!part || !confirm('메모를 삭제하시겠습니까?')) return
    
    setSaving(true)
    try {
      await deletePartNote(part.id)
      setNote('')
      setEditing(false)
      if (onUpdate) {
        onUpdate()
      }
      // 부품 정보 갱신
      const updatedPart = await getPartById(part.id)
      setPart(updatedPart)
    } catch (error) {
      console.error('메모 삭제 실패:', error)
      alert('메모 삭제에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (!part) {
    return null
  }

  return (
    <div className="part-detail">
      <h2>부품 상세 정보</h2>
      
      <div className="part-info">
        <div className="info-row">
          <label>이름:</label>
          <span>{part.name || `부품 ${part.id}`}</span>
        </div>
        
        <div className="info-row">
          <label>위치:</label>
          <span>
            X: {part.positionX?.toFixed(2) || 'N/A'}, 
            Y: {part.positionY?.toFixed(2) || 'N/A'}, 
            Z: {part.positionZ?.toFixed(2) || 'N/A'}
          </span>
        </div>
        
        <div className="info-row">
          <label>크기:</label>
          <span>
            X: {part.sizeX?.toFixed(2) || 'N/A'}, 
            Y: {part.sizeY?.toFixed(2) || 'N/A'}, 
            Z: {part.sizeZ?.toFixed(2) || 'N/A'}
          </span>
        </div>
      </div>

      <div className="part-note-section">
        <h3>메모</h3>
        {editing ? (
          <div className="note-editor">
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="메모를 입력하세요..."
              rows={4}
            />
            <div className="note-actions">
              <button onClick={handleSaveNote} disabled={saving}>
                {saving ? '저장 중...' : '저장'}
              </button>
              <button onClick={() => {
                setEditing(false)
                setNote(part.note || '')
              }} disabled={saving}>
                취소
              </button>
            </div>
          </div>
        ) : (
          <div className="note-display">
            {part.note ? (
              <p>{part.note}</p>
            ) : (
              <p className="no-note">메모가 없습니다.</p>
            )}
            <div className="note-actions">
              <button onClick={() => setEditing(true)}>수정</button>
              {part.note && (
                <button onClick={handleDeleteNote} className="delete-button">
                  삭제
                </button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

export default PartDetail
