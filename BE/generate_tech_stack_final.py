#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
기술 스택 다이어그램 생성 (로고 아이콘 포함)
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch, FancyArrowPatch, Circle, Rectangle, Polygon
import matplotlib.font_manager as fm
import numpy as np

# 한글 폰트 설정
plt.rcParams['font.family'] = 'Malgun Gothic'  # Windows
plt.rcParams['axes.unicode_minus'] = False

# 각 기술의 공식 색상, 아이콘 심볼 및 정보
TECH_INFO = {
    # 프론트엔드
    'React': {'color': '#61DAFB', 'bg': '#20232A', 'text': 'white', 'icon': 'R', 'symbol': 'R'},
    'TypeScript': {'color': '#3178C6', 'bg': '#3178C6', 'text': 'white', 'icon': 'TS', 'symbol': 'TS'},
    'Vite': {'color': '#646CFF', 'bg': '#646CFF', 'text': 'white', 'icon': 'V', 'symbol': 'V'},
    'React Router': {'color': '#CA4245', 'bg': '#CA4245', 'text': 'white', 'icon': 'RR', 'symbol': 'RR'},
    'ApexCharts': {'color': '#FF4560', 'bg': '#FF4560', 'text': 'white', 'icon': 'AC', 'symbol': 'AC'},
    'Three.js': {'color': '#000000', 'bg': '#000000', 'text': 'white', 'icon': '3D', 'symbol': '3D'},
    'Sass': {'color': '#CC6699', 'bg': '#CC6699', 'text': 'white', 'icon': 'S', 'symbol': 'S'},
    'Axios': {'color': '#5A29E4', 'bg': '#5A29E4', 'text': 'white', 'icon': 'AX', 'symbol': 'AX'},
    'Zustand': {'color': '#443C85', 'bg': '#443C85', 'text': 'white', 'icon': 'Z', 'symbol': 'Z'},
    'React Grid Layout': {'color': '#61DAFB', 'bg': '#20232A', 'text': 'white', 'icon': 'GL', 'symbol': 'GL'},
    'EventSource API': {'color': '#4A90E2', 'bg': '#4A90E2', 'text': 'white', 'icon': 'SSE', 'symbol': 'SSE'},
    
    # 백엔드
    'Spring Boot': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'SB', 'symbol': 'SB'},
    'Java': {'color': '#ED8B00', 'bg': '#ED8B00', 'text': 'white', 'icon': 'J', 'symbol': 'J'},
    'Spring Batch': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'B', 'symbol': 'B'},
    'Spring Security': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'SS', 'symbol': 'SS'},
    'Spring Data JPA': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'JPA', 'symbol': 'JPA'},
    'QueryDSL': {'color': '#1C4E80', 'bg': '#1C4E80', 'text': 'white', 'icon': 'Q', 'symbol': 'Q'},
    'Java Mail': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'MAIL', 'symbol': 'MAIL'},
    'SseEmitter': {'color': '#6DB33F', 'bg': '#6DB33F', 'text': 'white', 'icon': 'SSE', 'symbol': 'SSE'},
    'Apache POI': {'color': '#FF6B35', 'bg': '#FF6B35', 'text': 'white', 'icon': 'POI', 'symbol': 'POI'},
    'JFreeChart': {'color': '#1B75BC', 'bg': '#1B75BC', 'text': 'white', 'icon': 'CH', 'symbol': 'CH'},
    
    # 데이터베이스
    'Oracle': {'color': '#F80000', 'bg': '#F80000', 'text': 'white', 'icon': 'OR', 'symbol': 'OR'},
    'PL/SQL': {'color': '#F80000', 'bg': '#F80000', 'text': 'white', 'icon': 'PL', 'symbol': 'PL'},
    'Redis': {'color': '#DC382D', 'bg': '#DC382D', 'text': 'white', 'icon': 'RD', 'symbol': 'RD'},
    
    # 인프라
    'Docker': {'color': '#2496ED', 'bg': '#2496ED', 'text': 'white', 'icon': 'D', 'symbol': 'D'},
    'Gradle': {'color': '#02303A', 'bg': '#02303A', 'text': 'white', 'icon': 'G', 'symbol': 'GR'},
    'Swagger': {'color': '#85EA2D', 'bg': '#1B5E20', 'text': 'white', 'icon': 'SW', 'symbol': 'SW'},
    'Postman': {'color': '#FF6C37', 'bg': '#FF6C37', 'text': 'white', 'icon': 'PM', 'symbol': 'PM'},
    
    # 외부 API
    'Slack Webhook': {'color': '#4A154B', 'bg': '#4A154B', 'text': 'white', 'icon': 'SL', 'symbol': 'SL'},
    'OpenAI': {'color': '#10A37F', 'bg': '#10A37F', 'text': 'white', 'icon': 'AI', 'symbol': 'AI'},
}

def draw_logo_icon(ax, x, y, width, height, tech_info, tech_name):
    """로고 아이콘 그리기"""
    center_x = x + width / 2
    center_y = y + height / 2
    
    # 배경 박스
    logo_box = FancyBboxPatch((x, y), width, height,
                              boxstyle="round,pad=0.05",
                              edgecolor=tech_info['color'], linewidth=2.5,
                              facecolor=tech_info['bg'], alpha=1.0)
    ax.add_patch(logo_box)
    
    # 아이콘 텍스트 (심볼)
    icon_text = tech_info.get('icon', tech_info.get('symbol', tech_name[0]))
    # 아이콘 크기 조정 (긴 텍스트는 작게)
    icon_size = 14 if len(icon_text) <= 2 else 10
    ax.text(center_x, center_y, icon_text,
            ha='center', va='center', fontsize=icon_size, fontweight='bold',
            color=tech_info['text'])

def create_tech_stack_diagram():
    """기술 스택 다이어그램 생성 (로고 아이콘 포함)"""
    fig, ax = plt.subplots(1, 1, figsize=(20, 18))
    ax.set_xlim(0, 20)
    ax.set_ylim(0, 20)
    ax.axis('off')
    
    # 배경 그라데이션 효과
    ax.add_patch(Rectangle((0, 0), 20, 20, facecolor='#F8F9FA', alpha=0.3))
    
    # 제목
    title_box = FancyBboxPatch((1.5, 18.5), 17, 1,
                              boxstyle="round,pad=0.2",
                              edgecolor='#2C3E50', linewidth=3,
                              facecolor='white', alpha=0.95)
    ax.add_patch(title_box)
    ax.text(10, 19, 'DB Monitor 시스템 기술 스택', 
            ha='center', va='center', fontsize=30, fontweight='bold', color='#2C3E50')
    
    # 프론트엔드 레이어
    frontend_y = 16
    fe_header = FancyBboxPatch((0.5, frontend_y), 19, 0.7,
                               boxstyle="round,pad=0.1",
                               edgecolor='#496EF1', linewidth=2.5,
                               facecolor='#E5EEFF', alpha=0.9)
    ax.add_patch(fe_header)
    ax.text(10, frontend_y + 0.35, '프론트엔드 (Frontend)', 
            ha='center', va='center', fontsize=22, fontweight='bold', color='#2952E1')
    
    # 프론트엔드 기술들 (EventSource API 추가, 11개)
    fe_techs = [
        ('React', '19.1.1', 'React'),
        ('TypeScript', '5.9.3', 'TypeScript'),
        ('Vite', '7.1.7', 'Vite'),
        ('React Router', '7.9.4', 'React Router'),
        ('ApexCharts', '5.3.5', 'ApexCharts'),
        ('Three.js', '0.180.0', 'Three.js'),
        ('Sass', '1.93.2', 'Sass'),
        ('Axios', '1.13.2', 'Axios'),
        ('Zustand', 'latest', 'Zustand'),
        ('React Grid Layout', '1.5.2', 'React Grid Layout'),
        ('EventSource API', 'native', 'EventSource API'),
    ]
    
    fe_x_start = 0.8
    fe_y_start = frontend_y - 2.8
    fe_logo_size = 0.4
    fe_width = 1.75
    fe_height = 0.7
    fe_spacing_x = 1.9
    fe_spacing_y = 0.85
    
    for i, (name, version, key) in enumerate(fe_techs):
        row = i // 5
        col = i % 5
        x = fe_x_start + col * fe_spacing_x
        y = fe_y_start - row * fe_spacing_y
        
        tech_info = TECH_INFO.get(key, {'color': '#666666', 'bg': '#666666', 'text': 'white', 'icon': '?', 'symbol': '?'})
        
        # 로고 아이콘
        draw_logo_icon(ax, x, y, fe_logo_size, fe_height, tech_info, name)
        
        # 기술명 박스
        tech_box = FancyBboxPatch((x + fe_logo_size + 0.05, y), fe_width - fe_logo_size - 0.05, fe_height,
                                  boxstyle="round,pad=0.05",
                                  edgecolor=tech_info['color'], linewidth=2,
                                  facecolor='white', alpha=0.98)
        ax.add_patch(tech_box)
        
        # 기술명 (긴 이름은 폰트 크기 조정)
        name_fontsize = 9 if len(name) > 12 else 10
        ax.text(x + fe_logo_size + 0.1 + (fe_width - fe_logo_size - 0.05)/2, y + fe_height - 0.25, name,
                ha='center', va='center', fontsize=name_fontsize, fontweight='bold',
                color=tech_info['color'])
        
        # 버전
        ax.text(x + fe_logo_size + 0.1 + (fe_width - fe_logo_size - 0.05)/2, y + 0.15, f'v{version}',
                ha='center', va='center', fontsize=7, color='#888888', style='italic')
    
    # 백엔드 레이어
    backend_y = 11.5
    be_header = FancyBboxPatch((0.5, backend_y), 19, 0.7,
                               boxstyle="round,pad=0.1",
                               edgecolor='#16A34A', linewidth=2.5,
                               facecolor='#E9FFE9', alpha=0.9)
    ax.add_patch(be_header)
    ax.text(10, backend_y + 0.35, '백엔드 (Backend)', 
            ha='center', va='center', fontsize=22, fontweight='bold', color='#09714B')
    
    # 백엔드 기술들 (Apache POI, JFreeChart 추가)
    be_techs = [
        ('Spring Boot', '3.5.6', 'Spring Boot'),
        ('Java', '17', 'Java'),
        ('Spring Batch', 'latest', 'Spring Batch'),
        ('Spring Security', 'latest', 'Spring Security'),
        ('Spring Data JPA', 'latest', 'Spring Data JPA'),
        ('QueryDSL', '5.0.0', 'QueryDSL'),
        ('Java Mail', 'latest', 'Java Mail'),
        ('Spring Web MVC\nSseEmitter', 'latest', 'SseEmitter'),
        ('Apache POI', '5.2.5', 'Apache POI'),
        ('JFreeChart', '1.5.3', 'JFreeChart'),
    ]
    
    be_x_start = 1.5
    be_y_start = backend_y - 3
    be_logo_size = 0.4
    be_width = 2
    be_height = 0.75
    be_spacing_x = 2.5
    be_spacing_y = 0.9
    
    for i, (name, version, key) in enumerate(be_techs):
        row = i // 4
        col = i % 4
        x = be_x_start + col * be_spacing_x
        y = be_y_start - row * be_spacing_y
        
        tech_info = TECH_INFO.get(key, {'color': '#666666', 'bg': '#666666', 'text': 'white', 'icon': '?', 'symbol': '?'})
        
        # 로고 아이콘
        draw_logo_icon(ax, x, y, be_logo_size, be_height, tech_info, name)
        
        # 기술명 박스
        tech_box = FancyBboxPatch((x + be_logo_size + 0.05, y), be_width - be_logo_size - 0.05, be_height,
                                  boxstyle="round,pad=0.05",
                                  edgecolor=tech_info['color'], linewidth=2,
                                  facecolor='white', alpha=0.98)
        ax.add_patch(tech_box)
        
        # 기술명 (줄바꿈 처리)
        name_lines = name.split('\n')
        if len(name_lines) == 1:
            name_fontsize = 9 if len(name) > 15 else 10
            ax.text(x + be_logo_size + 0.1 + (be_width - be_logo_size - 0.05)/2, y + be_height - 0.25, name,
                    ha='center', va='center', fontsize=name_fontsize, fontweight='bold',
                    color=tech_info['color'])
        else:
            ax.text(x + be_logo_size + 0.1 + (be_width - be_logo_size - 0.05)/2, y + be_height - 0.3, name_lines[0],
                    ha='center', va='center', fontsize=9, fontweight='bold',
                    color=tech_info['color'])
            ax.text(x + be_logo_size + 0.1 + (be_width - be_logo_size - 0.05)/2, y + be_height - 0.55, name_lines[1],
                    ha='center', va='center', fontsize=9, fontweight='bold',
                    color=tech_info['color'])
        
        # 버전
        ax.text(x + be_logo_size + 0.1 + (be_width - be_logo_size - 0.05)/2, y + 0.15, f'v{version}',
                ha='center', va='center', fontsize=7, color='#888888', style='italic')
    
    # 데이터베이스 레이어
    db_y = 7
    db_header = FancyBboxPatch((0.5, db_y), 19, 0.7,
                               boxstyle="round,pad=0.1",
                               edgecolor='#F80000', linewidth=2.5,
                               facecolor='#FFE5E5', alpha=0.9)
    ax.add_patch(db_header)
    ax.text(10, db_y + 0.35, '데이터베이스 (Database)', 
            ha='center', va='center', fontsize=22, fontweight='bold', color='#CC0000')
    
    # 데이터베이스 기술들 (PostgreSQL 제거, 3개만 남음)
    db_techs = [
        ('Oracle Database', '21c', 'Oracle'),
        ('PL/SQL', 'Oracle', 'PL/SQL'),
        ('Redis', 'latest', 'Redis'),
    ]
    
    db_x_start = 3.5
    db_y_start = db_y - 1.5
    db_logo_size = 0.45
    db_width = 3
    db_height = 0.8
    db_spacing_x = 4.5
    
    for i, (name, version, key) in enumerate(db_techs):
        x = db_x_start + i * db_spacing_x
        y = db_y_start
        
        tech_info = TECH_INFO.get(key, {'color': '#666666', 'bg': '#666666', 'text': 'white', 'icon': '?', 'symbol': '?'})
        
        # 로고 아이콘
        draw_logo_icon(ax, x, y, db_logo_size, db_height, tech_info, name)
        
        # 기술명 박스
        tech_box = FancyBboxPatch((x + db_logo_size + 0.05, y), db_width - db_logo_size - 0.05, db_height,
                                  boxstyle="round,pad=0.05",
                                  edgecolor=tech_info['color'], linewidth=2.5,
                                  facecolor='white', alpha=0.98)
        ax.add_patch(tech_box)
        
        # 기술명
        ax.text(x + db_logo_size + 0.1 + (db_width - db_logo_size - 0.05)/2, y + db_height - 0.3, name,
                ha='center', va='center', fontsize=11, fontweight='bold',
                color=tech_info['color'])
        
        # 버전
        ax.text(x + db_logo_size + 0.1 + (db_width - db_logo_size - 0.05)/2, y + 0.25, version,
                ha='center', va='center', fontsize=9, color='#888888', style='italic')
    
    # 외부 API 레이어 (새로 추가)
    api_y = 4.5
    api_header = FancyBboxPatch((0.5, api_y), 19, 0.7,
                                boxstyle="round,pad=0.1",
                                edgecolor='#4A154B', linewidth=2.5,
                                facecolor='#F5E6FA', alpha=0.9)
    ax.add_patch(api_header)
    ax.text(10, api_y + 0.35, '외부 API (External APIs)', 
            ha='center', va='center', fontsize=22, fontweight='bold', color='#4A154B')
    
    # 외부 API 기술들 (OpenAI 추가, 2개)
    api_techs = [
        ('Slack Webhook API', 'RestTemplate', 'Slack Webhook'),
        ('OpenAI API', 'Spring AI', 'OpenAI'),
    ]
    
    api_x_start = 4
    api_y_start = api_y - 1.2
    api_logo_size = 0.45
    api_width = 3.2
    api_height = 0.8
    api_spacing_x = 5
    
    for i, (name, version, key) in enumerate(api_techs):
        x = api_x_start + i * api_spacing_x
        y = api_y_start
        
        tech_info = TECH_INFO.get(key, {'color': '#666666', 'bg': '#666666', 'text': 'white', 'icon': '?', 'symbol': '?'})
        
        # 로고 아이콘
        draw_logo_icon(ax, x, y, api_logo_size, api_height, tech_info, name)
        
        # 기술명 박스
        tech_box = FancyBboxPatch((x + api_logo_size + 0.05, y), api_width - api_logo_size - 0.05, api_height,
                                  boxstyle="round,pad=0.05",
                                  edgecolor=tech_info['color'], linewidth=2.5,
                                  facecolor='white', alpha=0.98)
        ax.add_patch(tech_box)
        
        # 기술명
        ax.text(x + api_logo_size + 0.1 + (api_width - api_logo_size - 0.05)/2, y + api_height - 0.3, name,
                ha='center', va='center', fontsize=11, fontweight='bold',
                color=tech_info['color'])
        
        # 설명
        ax.text(x + api_logo_size + 0.1 + (api_width - api_logo_size - 0.05)/2, y + 0.25, version,
                ha='center', va='center', fontsize=9, color='#888888', style='italic')
    
    # 인프라 레이어
    infra_y = 2
    infra_header = FancyBboxPatch((0.5, infra_y), 19, 0.7,
                                  boxstyle="round,pad=0.1",
                                  edgecolor='#8B5CF6', linewidth=2.5,
                                  facecolor='#F5F3FF', alpha=0.9)
    ax.add_patch(infra_header)
    ax.text(10, infra_y + 0.35, '인프라 & 도구 (Infrastructure)', 
            ha='center', va='center', fontsize=22, fontweight='bold', color='#6D28D9')
    
    # 인프라 기술들 (Swagger로 변경, Postman 추가, Apache POI/JFreeChart 제거)
    infra_techs = [
        ('Docker', 'latest', 'Docker'),
        ('Gradle', 'latest', 'Gradle'),
        ('Swagger', '2.8.8', 'Swagger'),
        ('Postman', 'latest', 'Postman'),
    ]
    
    infra_x_start = 2.5
    infra_y_start = infra_y - 1.5
    infra_logo_size = 0.4
    infra_width = 2.5
    infra_height = 0.75
    infra_spacing_x = 3.5
    
    for i, (name, version, key) in enumerate(infra_techs):
        x = infra_x_start + i * infra_spacing_x
        y = infra_y_start
        
        tech_info = TECH_INFO.get(key, {'color': '#666666', 'bg': '#666666', 'text': 'white', 'icon': '?', 'symbol': '?'})
        
        # 로고 아이콘
        draw_logo_icon(ax, x, y, infra_logo_size, infra_height, tech_info, name)
        
        # 기술명 박스
        tech_box = FancyBboxPatch((x + infra_logo_size + 0.05, y), infra_width - infra_logo_size - 0.05, infra_height,
                                  boxstyle="round,pad=0.05",
                                  edgecolor=tech_info['color'], linewidth=2.5,
                                  facecolor='white', alpha=0.98)
        ax.add_patch(tech_box)
        
        # 기술명
        ax.text(x + infra_logo_size + 0.1 + (infra_width - infra_logo_size - 0.05)/2, y + infra_height - 0.3, name,
                ha='center', va='center', fontsize=10, fontweight='bold',
                color=tech_info['color'])
        
        # 버전
        ax.text(x + infra_logo_size + 0.1 + (infra_width - infra_logo_size - 0.05)/2, y + 0.2, f'v{version}',
                ha='center', va='center', fontsize=8, color='#888888', style='italic')
    
    # 연결선 (겹치지 않도록 위치 조정)
    # 프론트엔드 -> 백엔드 (중앙)
    arrow1 = FancyArrowPatch((10, frontend_y - 2.5), (10, backend_y + 0.7),
                            arrowstyle='->', mutation_scale=25,
                            linewidth=2.5, color='#666666', alpha=0.4,
                            linestyle='--', zorder=1)
    ax.add_patch(arrow1)
    
    # 백엔드 -> 데이터베이스 (중앙)
    arrow2 = FancyArrowPatch((10, backend_y - 3), (10, db_y + 0.7),
                            arrowstyle='->', mutation_scale=25,
                            linewidth=2.5, color='#666666', alpha=0.4,
                            linestyle='--', zorder=1)
    ax.add_patch(arrow2)
    
    # 백엔드 -> Slack Webhook (왼쪽)
    arrow3 = FancyArrowPatch((7, backend_y - 1.5), (6.5, api_y + 0.7),
                            arrowstyle='->', mutation_scale=25,
                            linewidth=2.5, color='#4A154B', alpha=0.6,
                            linestyle='--', zorder=1)
    ax.add_patch(arrow3)
    ax.text(5.5, (backend_y - 1.5 + api_y + 0.7)/2, 'CRITICAL\n알림', 
            ha='right', va='center', fontsize=8, color='#4A154B', style='italic',
            bbox=dict(boxstyle='round,pad=0.3', facecolor='white', alpha=0.8, edgecolor='none'))
    
    # 백엔드 -> OpenAI (오른쪽)
    arrow5 = FancyArrowPatch((13, backend_y - 1.5), (13.5, api_y + 0.7),
                            arrowstyle='->', mutation_scale=25,
                            linewidth=2.5, color='#10A37F', alpha=0.6,
                            linestyle='--', zorder=1)
    ax.add_patch(arrow5)
    ax.text(14.5, (backend_y - 1.5 + api_y + 0.7)/2, 'AI\n분석', 
            ha='left', va='center', fontsize=8, color='#10A37F', style='italic',
            bbox=dict(boxstyle='round,pad=0.3', facecolor='white', alpha=0.8, edgecolor='none'))
    
    # 데이터베이스 -> 인프라 (중앙)
    arrow4 = FancyArrowPatch((10, db_y - 1.5), (10, infra_y + 0.7),
                            arrowstyle='->', mutation_scale=25,
                            linewidth=2.5, color='#666666', alpha=0.4,
                            linestyle='--', zorder=1)
    ax.add_patch(arrow4)
    
    # 하단 설명
    desc_text = "각 기술의 공식 브랜드 색상과 아이콘을 적용하여 시각적으로 구분"
    ax.text(10, 0.5, desc_text, ha='center', va='center',
            fontsize=11, color='#666666', style='italic')
    
    plt.tight_layout()
    plt.savefig('tech_stack.png', dpi=300, bbox_inches='tight', facecolor='white', pad_inches=0.2)
    print("기술 스택 다이어그램이 'tech_stack.png'로 저장되었습니다.")
    plt.close()

if __name__ == '__main__':
    print("기술 스택 다이어그램 생성 중...")
    create_tech_stack_diagram()
    print("완료!")

