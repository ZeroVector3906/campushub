export type ClubHistoryCategory = '发展历程' | '赛事荣誉'

export interface ClubHistoryItem {
  year: string
  category: ClubHistoryCategory
  title: string
  summary: string
}

// Curated from the club introduction, development history, award records, and culture deck.
export const clubHistoryItems: ClubHistoryItem[] = [
  { year: '2013', category: '发展历程', title: '征途从校园出发', summary: '征途单车俱乐部于世界无车日由师生共同发起，设立于西安欧亚学院通识教育学院体育中心。' },
  { year: '2014', category: '发展历程', title: '陕西省高校自行车联盟成立', summary: '联盟在西安欧亚学院成立，由征途牵头，多所西安高校自行车协会参与。' },
  { year: '2015', category: '发展历程', title: '首届陕西省高校自行车交流赛', summary: '赛事在西安欧亚学院举办，俱乐部简介记载参赛人数 600 余人。' },
  { year: '2016', category: '发展历程', title: '承办骑闯天路西安挑战赛', summary: '依托西安欧亚学院举办赛事，并组建欧亚征途车队，开始参加专业自行车赛事。' },
  { year: '2017', category: '发展历程', title: '骑沪南下，约 1500 公里', summary: '车队从西安出发前往上海同济大学交流，以长途骑行连接高校骑行社群。' },
  { year: '2018', category: '发展历程', title: '获授高校自行车训练基地', summary: '中国大学生体育协会自行车分会授予西安欧亚学院和西安交通大学自行车训练基地。' },
  { year: '2019', category: '发展历程', title: '一带一路 · 横贯欧亚', summary: '开展“逐梦敦煌”丝路高校联动骑行活动。' },
  { year: '2021', category: '发展历程', title: '参与第十四届全运会执裁', summary: '14 名欧亚师生和校友参与小轮车、公路自行车、山地自行车项目执裁。' },
  { year: '2021', category: '赛事荣誉', title: '中国大学生室内自行车云上挑战赛', summary: '征途成员获得男子 3 公里计时赛亚军。' },
  { year: '2025', category: '赛事荣誉', title: '全国大学生自行车锦标赛', summary: '征途成员李雅泽获得场地 400 米淘汰赛亚军。' },
  { year: '2026', category: '赛事荣誉', title: '国际大学生自行车赛事两项季军', summary: '征途成员李雅泽获得混合团体接力计时赛、女子团体冲刺赛第三名。' },
  { year: '2026', category: '赛事荣誉', title: '全国大学生自行车锦标赛', summary: '王晴天、刘书豪、方子睿获得场地男子团体竞速赛第三名。' },
]
