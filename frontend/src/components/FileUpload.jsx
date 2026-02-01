import { useRef } from 'react'

function FileUpload({ onUpload, loading }) {
  const fileInputRef = useRef(null)

  const handleFileChange = (e) => {
    const file = e.target.files[0]
    if (file) {
      const extension = file.name.split('.').pop().toLowerCase()
      const allowedExtensions = ['stl', 'obj', 'ply', 'step', 'stp', 'igs', 'iges']
      if (allowedExtensions.includes(extension)) {
        onUpload(file)
      } else {
        alert('STL, OBJ, PLY, STEP, STP, IGES 파일만 업로드 가능합니다.')
      }
    }
    // 같은 파일을 다시 선택할 수 있도록 리셋
    e.target.value = ''
  }

  const handleClick = () => {
    fileInputRef.current?.click()
  }

  return (
    <div className="file-upload">
      <h2>파일 업로드</h2>
      <input
        ref={fileInputRef}
        type="file"
        accept=".stl,.obj,.ply,.step,.stp,.igs,.iges"
        onChange={handleFileChange}
        style={{ display: 'none' }}
      />
      <button
        onClick={handleClick}
        disabled={loading}
        className="upload-button"
      >
        {loading ? '업로드 중...' : 'CAD 파일 선택'}
      </button>
      <p className="upload-hint">STL, OBJ, PLY, STEP, STP, IGES 파일 지원</p>
    </div>
  )
}

export default FileUpload
