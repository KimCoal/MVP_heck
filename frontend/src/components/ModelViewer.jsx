import { Suspense, useEffect, useState, useRef } from 'react'
import { Canvas, useThree } from '@react-three/fiber'
import { OrbitControls, useGLTF, Grid } from '@react-three/drei'
import { getGlbFileUrl } from '../api/cadApi'
import * as THREE from 'three'

// 카메라 자동 조정 컴포넌트
function CameraController({ sceneRef }) {
    const { camera, size } = useThree()
    const controlsRef = useRef()
    const [initialized, setInitialized] = useState(false)

    useEffect(() => {
        if (!sceneRef?.current || initialized) return

        // 바운딩 박스 계산
        const box = new THREE.Box3().setFromObject(sceneRef.current)
        const center = box.getCenter(new THREE.Vector3())
        const boxSize = box.getSize(new THREE.Vector3())

        // 가장 긴 축 찾기
        const maxDim = Math.max(boxSize.x, boxSize.y, boxSize.z)
        if (maxDim === 0) return

        // 화면 비율 고려
        const aspect = size.width / size.height
        const fov = camera.fov * (Math.PI / 180)

        // 카메라 거리 계산 (화면에 모델이 꽉 차도록)
        let distance
        if (aspect > 1) {
            // 가로가 더 긴 경우
            distance = Math.abs(maxDim / (2 * Math.tan(fov / 2)))
        } else {
            // 세로가 더 긴 경우
            distance = Math.abs(maxDim / (2 * Math.tan(fov / 2) / aspect))
        }

        // 여유 공간 추가 (10%)
        distance *= 1.1

        // 카메라 위치 설정 (대각선 방향)
        const offset = distance * 0.8
        camera.position.set(
            center.x + offset,
            center.y + offset,
            center.z + offset
        )

        // 카메라가 모델 중심을 보도록 설정
        camera.lookAt(center)
        camera.updateProjectionMatrix()

        // 컨트롤 타겟 설정
        if (controlsRef.current) {
            controlsRef.current.target.copy(center)
            controlsRef.current.update()
        }

        setInitialized(true)
    }, [sceneRef, camera, size, initialized])

    return <OrbitControls
        ref={controlsRef}
        enableDamping
        dampingFactor={0.05}
        minDistance={0.1}
        maxDistance={10000}
    />
}

function Model({ url, parts, onPartClick, selectedPartId, sceneRef }) {
    const { scene } = useGLTF(url)
    const [hoveredMesh, setHoveredMesh] = useState(null)

    useEffect(() => {
        if (scene && sceneRef) {
            sceneRef.current = scene
        }
    }, [scene, sceneRef])

    useEffect(() => {
        if (!scene) return

        const meshes = []
        scene.traverse((child) => {
            if (child.isMesh) {
                meshes.push(child)

                // 원본 머티리얼과 색상 보존
                if (!child.userData.originalMaterial) {
                    // 원본 머티리얼 저장 (배열인 경우 처리)
                    if (Array.isArray(child.material)) {
                        child.userData.originalMaterial = child.material.map(mat => mat.clone())
                        child.userData.originalColor = child.material.map(mat =>
                            mat.color ? mat.color.clone() : new THREE.Color(0xffffff)
                        )
                    } else {
                        child.userData.originalMaterial = child.material ? child.material.clone() : null
                        child.userData.originalColor = child.material?.color
                            ? child.material.color.clone()
                            : new THREE.Color(0xffffff)
                    }
                }

                // 원본 머티리얼이 있으면 그대로 사용, 없으면 기본 머티리얼 생성
                if (!child.material) {
                    child.material = new THREE.MeshStandardMaterial({
                        color: child.userData.originalColor || '#ffffff',
                        metalness: 0.2,
                        roughness: 0.8,
                    })
                }

                child.castShadow = true
                child.receiveShadow = true
            }
        })

        const handleClick = (event) => {
            event.stopPropagation()
            const mesh = event.object

            // 부품 찾기 (이름으로 매칭)
            if (mesh.name && parts) {
                const part = parts.find(p =>
                    p.name === mesh.name ||
                    mesh.name.includes(p.name) ||
                    p.name.includes(mesh.name)
                )
                if (part) {
                    onPartClick(part)
                }
            }
        }

        const handlePointerOver = (event) => {
            event.stopPropagation()
            setHoveredMesh(event.object)
            document.body.style.cursor = 'pointer'
        }

        const handlePointerOut = () => {
            setHoveredMesh(null)
            document.body.style.cursor = 'default'
        }

        // 이벤트 리스너 추가
        meshes.forEach(mesh => {
            mesh.addEventListener('click', handleClick)
            mesh.addEventListener('pointerover', handlePointerOver)
            mesh.addEventListener('pointerout', handlePointerOut)
        })

        return () => {
            meshes.forEach(mesh => {
                mesh.removeEventListener('click', handleClick)
                mesh.removeEventListener('pointerover', handlePointerOver)
                mesh.removeEventListener('pointerout', handlePointerOut)
            })
        }
    }, [scene, parts, onPartClick])

    // 선택 및 호버 효과 업데이트
    useEffect(() => {
        if (!scene) return

        scene.traverse((child) => {
            if (child.isMesh && child.material) {
                // 머티리얼이 배열인 경우 처리
                const materials = Array.isArray(child.material) ? child.material : [child.material]
                const originalColors = Array.isArray(child.userData.originalColor)
                    ? child.userData.originalColor
                    : [child.userData.originalColor || new THREE.Color(0xffffff)]

                materials.forEach((material, index) => {
                    if (!material || !material.color) return

                    // 원본 색상 가져오기
                    const originalColor = originalColors[index] ||
                        (material.color ? material.color.clone() : new THREE.Color(0xffffff))

                    let targetColor = originalColor.clone()

                    // 선택된 부품 하이라이트
                    if (selectedPartId && parts) {
                        const part = parts.find(p => p.id === selectedPartId)
                        if (part && (child.name === part.name ||
                            child.name.includes(part.name) ||
                            part.name.includes(child.name))) {
                            // 원본 색상을 밝게 하여 하이라이트
                            targetColor = originalColor.clone().lerp(new THREE.Color('#00ff00'), 0.5)
                        }
                    }

                    // 호버 효과
                    if (hoveredMesh === child) {
                        // 원본 색상을 주황색으로 보간하여 하이라이트
                        targetColor = originalColor.clone().lerp(new THREE.Color('#ff8800'), 0.4)
                    }

                    material.color.copy(targetColor)
                })
            }
        })
    }, [scene, parts, selectedPartId, hoveredMesh])

    if (!scene) return null

    return <primitive object={scene} />
}

function ModelViewer({ cadFileId, parts, onPartClick, selectedPartId }) {
    const [glbUrl, setGlbUrl] = useState(null)
    const sceneRef = useRef(null)

    useEffect(() => {
        if (cadFileId) {
            const url = getGlbFileUrl(cadFileId)
            console.log('GLB 파일 URL:', url)
            setGlbUrl(url)
            // 새로운 파일 로드 시 초기화 플래그 리셋
            sceneRef.current = null
        }
    }, [cadFileId])

    if (!glbUrl) {
        return <div className="model-viewer-loading">GLB 파일 로딩 중...</div>
    }

    return (
        <div className="model-viewer">
            <Canvas
                shadows
                gl={{ antialias: true }}
                camera={{ fov: 50, near: 0.1, far: 10000 }}
            >
                <color attach="background" args={['#f0f0f0']} />
                <ambientLight intensity={1.0} />
                <directionalLight position={[10, 10, 5]} intensity={1.5} castShadow />
                <directionalLight position={[-10, -10, -5]} intensity={0.8} />
                <Suspense fallback={null}>
                    <Model
                        url={glbUrl}
                        parts={parts}
                        onPartClick={onPartClick}
                        selectedPartId={selectedPartId}
                        sceneRef={sceneRef}
                    />
                    <CameraController sceneRef={sceneRef} />
                </Suspense>
                <Grid args={[10, 10]} cellColor="#cccccc" sectionColor="#999999" />
            </Canvas>
        </div>
    )
}

export default ModelViewer
