function start(ms) {
    var player = ms.getPlayer();
    var map = player.getMap();

    importPackage(Packages.server.life);
    importClass(android.graphics.Point);
    if (player.isCygnus()) {
        if (ms.isQuestStarted(20730) && ms.getQuestProgressInt(20730, 9300285) == 0) {
            map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(9300285), new Point(680, 258));
        }
    } else {
        if (ms.isQuestStarted(21731) && ms.getQuestProgressInt(21731, 9300344) == 0) {
            map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(9300344), new Point(680, 258));
        }
    }
}