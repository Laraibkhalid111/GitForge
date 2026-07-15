package com.gitforge.repository;

import com.gitforge.dao.SettingsDAO;
import com.gitforge.model.Settings;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Repository-pattern wrapper over {@link SettingsDAO}.
 */
public class SettingsRepository {

    private final SettingsDAO dao;

    public SettingsRepository() {
        this(new SettingsDAO());
    }

    public SettingsRepository(SettingsDAO dao) {
        this.dao = dao;
    }

    public long create(Settings settings) throws SQLException {
        return dao.create(settings);
    }

    public Optional<Settings> findById(long id) throws SQLException {
        return dao.findById(id);
    }

    public Optional<Settings> findByKey(String key) throws SQLException {
        return dao.findByKey(key);
    }

    public boolean update(Settings settings) throws SQLException {
        return dao.update(settings);
    }

    public boolean delete(long id) throws SQLException {
        return dao.delete(id);
    }

    public List<Settings> findAll() throws SQLException {
        return dao.findAll();
    }

    public List<Settings> search(String query) throws SQLException {
        return dao.search(query);
    }
}
