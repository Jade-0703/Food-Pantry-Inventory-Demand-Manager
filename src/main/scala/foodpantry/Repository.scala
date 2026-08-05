/**
 * ============================================================================
 *                          PERSISTENCE & REPOSITORIES
 * ============================================================================
 * Defines generic data access traits and SQL database persistence layers 
 * for storing data safely in SQLite database formats via JDBC.
 *
 * Implements:
 *   - S1-9 (Parametric Polymorphism via generics [T])
 *   - S1-10 (Encapsulation via private fields)
 *   - S1-12 (Exception Handling via scala.util.Try)
 * ============================================================================
 */
package foodpantry

import java.sql.{DriverManager, ResultSet}
import scala.util.Try

// ============================================================================
// 1. GENERIC ABSTRACTIONS
// ============================================================================

/**
 * S1-9 Parametric Polymorphism.
 * Generic boundary interface for storing and retrieving lists of records.
 */
trait Repository[T]:
  def loadAll(): Try[List[T]]
  def saveAll(items: List[T]): Try[Unit]

// ============================================================================
// 2. CONCRETE SQLITE JDBC PERSISTENCE LAYER
// ============================================================================

// ai-assisted: #3
// why: Assisted with implementing generic SQLite JDBC persistence repository layer for SQL storage.
/**
 * Generic SQLite repository implementation backing persistence onto a local SQL database via JDBC.
 */
class SqliteRepository[T](
  dbPathString: String,
  tableName: String,
  createTableSql: String,
  rowToItem: ResultSet => T,
  itemToParams: T => Seq[AnyRef]
) extends Repository[T]:

  // S1-10 Encapsulation: Private field with adjacent rationale comment
  // Rationale: Keep database JDBC connection URL private to prevent unauthorized URL manipulation
  private val dbUrl: String = s"jdbc:sqlite:$dbPathString"

  // Initialize SQL table schema on component startup
  Try {
    val conn = DriverManager.getConnection(dbUrl)
    try
      val stmt = conn.createStatement()
      try
        stmt.execute(createTableSql)
      finally
        stmt.close()
    finally
      conn.close()
  }

  /** Recursively reads JDBC result sets into an immutable List with zero vars. */
  private def readRows(rs: ResultSet): List[T] =
    if !rs.next() then List.empty[T]
    else rowToItem(rs) :: readRows(rs)

  /**
   * S1-12 Exception Handling.
   * Loads all records from the encapsulated SQLite database table via SQL SELECT query.
   */
  override def loadAll(): Try[List[T]] = Try {
    val conn = DriverManager.getConnection(dbUrl)
    try
      val stmt = conn.createStatement()
      try
        val rs = stmt.executeQuery(s"SELECT * FROM $tableName")
        try
          readRows(rs)
        finally
          rs.close()
      finally
        stmt.close()
    finally
      conn.close()
  }

  /**
   * S1-12 Exception Handling.
   * Flushes and inserts all items to the encapsulated SQLite table inside a single SQL transaction.
   */
  override def saveAll(items: List[T]): Try[Unit] = Try {
    val conn = DriverManager.getConnection(dbUrl)
    conn.setAutoCommit(false)
    try
      val deleteStmt = conn.createStatement()
      try
        deleteStmt.executeUpdate(s"DELETE FROM $tableName")
      finally
        deleteStmt.close()

      if items.nonEmpty then
        val sampleParams = itemToParams(items.head)
        val placeholders = sampleParams.map(_ => "?").mkString(", ")
        val insertSql = s"INSERT INTO $tableName VALUES ($placeholders)"
        val insertStmt = conn.prepareStatement(insertSql)
        try
          items.foreach { item =>
            val params = itemToParams(item)
            params.zipWithIndex.foreach { case (param, idx) =>
              insertStmt.setObject(idx + 1, param)
            }
            insertStmt.addBatch()
          }
          insertStmt.executeBatch()
        finally
          insertStmt.close()

      conn.commit()
    catch
      case ex: Throwable =>
        conn.rollback()
        throw ex
    finally
      conn.setAutoCommit(true)
      conn.close()
  }
