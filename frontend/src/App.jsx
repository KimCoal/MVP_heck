import { useState, useEffect } from 'react'
import './App.css'
import FileUpload from './components/FileUpload'
import ModelViewer from './components/ModelViewer'
import PartList from './components/PartList'
import PartDetail from './components/PartDetail'
import { getCadFiles, uploadCadFile, getCadFileById } from './services/api'

function App() {
  const [cadFiles, setCadFiles] = useState([])
  const [selectedFile, setSelectedFile] = useState(null)
  const [selectedPart, setSelectedPart] = useState(null)
  const [loading, setLoading] = useState(false)

  const loadCadFiles = async () => {
    try {
      const files = await getCadFiles()
      setCadFiles(files)
    } catch (error) {
      console.error('파일 목록 로드 실패:', error)
    }
  }

  useEffect(() => {
    loadCadFiles()
  }, [])

  // 선택된 파일이 처리 중이면 주기적으로 갱신
  useEffect(() => {
    if (!selectedFile) return
    
    const needsUpdate = !selectedFile.glbFilePath || 
                       selectedFile.status === 'UPLOADING' || 
                       selectedFile.status === 'PROCESSING'
    
    if (!needsUpdate) {
      console.log('파일 처리 완료:', selectedFile)
      return
    }

    console.log('파일 처리 중, 주기적 갱신 시작:', selectedFile)

    const interval = setInterval(async () => {
      try {
        const files = await getCadFiles()
        setCadFiles(files)
        
        // 선택된 파일 정보 갱신
        const updatedFile = files.find(f => f.id === selectedFile.id)
        if (updatedFile) {
          console.log('파일 정보 갱신:', updatedFile)
          setSelectedFile(updatedFile)
        }
      } catch (error) {
        console.error('파일 정보 갱신 실패:', error)
      }
    }, 2000) // 2초마다 체크

    return () => clearInterval(interval)
  }, [selectedFile?.id, selectedFile?.status, selectedFile?.glbFilePath])

  const handleFileUpload = async (file) => {
    setLoading(true)
    try {
      const uploadedFile = await uploadCadFile(file)
      await loadCadFiles()
      setSelectedFile(uploadedFile)
      setLoading(false)
    } catch (error) {
      console.error('파일 업로드 실패:', error)
      setLoading(false)
      const errorMessage = error.response?.data || error.message || '파일 업로드에 실패했습니다.'
      alert(`파일 업로드 실패: ${errorMessage}`)
    }
  }

  const handleFileSelect = async (file) => {
    try {
      // 최신 파일 정보 가져오기
      const updatedFile = await getCadFileById(file.id)
      setSelectedFile(updatedFile)
      setSelectedPart(null)
    } catch (error) {
      console.error('파일 정보 로드 실패:', error)
      setSelectedFile(file)
      setSelectedPart(null)
    }
  }

  const handlePartSelect = (part) => {
    setSelectedPart(part)
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>CAD 파일 뷰어</h1>
      </header>
      
      <div className="app-content">
        <div className="sidebar">
          <FileUpload onUpload={handleFileUpload} loading={loading} />
          
          <div className="file-list">
            <h2>업로드된 파일</h2>
            {cadFiles.map(file => (
              <div
                key={file.id}
                className={`file-item ${selectedFile?.id === file.id ? 'active' : ''}`}
                onClick={() => handleFileSelect(file)}
              >
                <div className="file-name">{file.originalFilename}</div>
                <div className="file-status">{file.status}</div>
              </div>
            ))}
          </div>

          {selectedFile && (
            <>
              <PartList
                cadFileId={selectedFile.id}
                parts={selectedFile.parts || []}
                onPartSelect={handlePartSelect}
                selectedPartId={selectedPart?.id}
              />
              {selectedPart && (
                <PartDetail
                  part={selectedPart}
                  onUpdate={loadCadFiles}
                />
              )}
            </>
          )}
        </div>

        <div className="main-content">
          {selectedFile && selectedFile.glbFilePath ? (
            <ModelViewer
              cadFileId={selectedFile.id}
              parts={selectedFile.parts || []}
              onPartClick={handlePartSelect}
              selectedPartId={selectedPart?.id}
            />
          ) : (
            <div className="empty-viewer">
              {selectedFile ? (
                <div>
                  <p>파일 처리 중...</p>
                  <p>상태: {selectedFile.status}</p>
                  <p style={{ fontSize: '0.9rem', color: '#999', marginTop: '10px' }}>
                    GLB 파일 경로: {selectedFile.glbFilePath || '없음'}
                  </p>
                  {selectedFile.status === 'FAILED' && (
                    <p style={{ color: '#ff4444', marginTop: '10px' }}>파일 처리에 실패했습니다.</p>
                  )}
                  {selectedFile.status === 'COMPLETED' && !selectedFile.glbFilePath && (
                    <p style={{ color: '#ff8800', marginTop: '10px' }}>
                      처리 완료되었지만 GLB 파일이 없습니다. 파일을 다시 선택해보세요.
                    </p>
                  )}
                </div>
              ) : (
                <p>파일을 선택하거나 업로드하세요.</p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

export default App
