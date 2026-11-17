# 水幻形实现合同报告

日期：2026-11-02。只读源码后写出合同，未改 `src`，未编译，未打包，未启动游戏，未改制作表，未提交，未操作美术 MCP，未读美术作者的实时工作。

## 写入

- `D:/alianximod/docs/开发记录/水幻形术式-20261102/implementation-contract.md`
- `D:/alianximod/docs/开发记录/水幻形术式-20261102/implementation-contract-report.md`

合同按已裁决设计冻结 A/B/C/D 的路径、记录构造、租约单向转移、报价/生成/冷却签名、tick 落点和致死六步的现有方法名。`water_phantom` 的注册、专项 `declareSpecials()` 和候选资源本次都不调用。

## 真实阻塞

有一处。`TechniqueType.MAGIC` 的水幻形和 `TechniqueType.SWORD` 的断潮，过不了现有装配和释放门：

- `SkillAssemblyPacket.put`：剑技槽拒绝法术，法杖拒绝剑术
- `SkillUsePacket.offerNow`：法杖链含剑术即拒绝
- `SpellAssemblyMenu.rejectReason` 与剑技同步：同样双向拒绝，同步时跳过法术

合同第 9 节记录了行号。本次没有改这些门，也没有把任一术式改成另一种类型。A/B 的核心与体积、C 的编译器和未接线类、D 的无贴图协议仍可按合同实施。玩家能装出「水幻形 → 断潮」要等主会话指定走哪一扇现有门。

没有第二处需要改架构的事实缺口。断潮现行画面是整张 UV 而不是静止帧条，僵尸贴图要等 D 的 `WATER_OUTLINE` 早退，封存会拒绝未登记术式的专项声明。这三件都已经写进合同的签名和等待项。

## 停止写入

本任务到此停止。不继续改合同以外的文件。
