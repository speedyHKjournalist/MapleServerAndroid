function start(ms) {

    if (ms.getQuestStatus(2175) == 1) {
        var mobId = 9300156;
        var player = ms.getPlayer();
        var map = player.getMap();

        if (map.getMonsterById(mobId) != null) {
            return;
        }

        importPackage(Packages.server.life);
        importClass(android.graphics.Point);
        map.spawnMonsterOnGroundBelow(LifeFactory.getMonster(mobId), new Point(-1027, 216));
    }
}